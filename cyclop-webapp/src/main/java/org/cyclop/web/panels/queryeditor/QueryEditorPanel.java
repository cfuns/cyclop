/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyclop.web.panels.queryeditor;

import javax.inject.Inject;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.cyclop.model.ContextCqlCompletion;
import org.cyclop.model.CqlQuery;
import org.cyclop.model.CqlQueryResult;
import org.cyclop.model.UserPreferences;
import org.cyclop.service.cassandra.QueryService;
import org.cyclop.service.exporter.CsvQueryResultExporter;
import org.cyclop.service.um.UserManager;
import org.cyclop.web.panels.queryeditor.buttons.ButtonsPanel;
import org.cyclop.web.panels.queryeditor.completionhint.CompletionHintPanel;
import org.cyclop.web.panels.queryeditor.cqlhelp.CqlHelpPanel;
import org.cyclop.web.panels.queryeditor.editor.CompletionChangeListener;
import org.cyclop.web.panels.queryeditor.editor.EditorPanel;
import org.cyclop.web.panels.queryeditor.export.QueryResultExport;
import org.cyclop.web.panels.queryeditor.result.QueryResultPanel;
import org.cyclop.web.panels.queryeditor.result.SwitchableQueryResultPanel;
import org.cyclop.web.panels.queryeditor.result.SwitchableQueryResultPanel.ViewType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Maciej Miklas */
@AuthorizeInstantiation(Roles.ADMIN)
public class QueryEditorPanel extends Panel {

	private final static Logger LOG = LoggerFactory.getLogger(QueryEditorPanel.class);

	private CqlHelpPanel cqlHelpPanel;

	private CompletionHintPanel cqlCompletionHintPanel;

	private boolean queryRunning = false;

	private CqlQuery lastQuery;

	private QueryResultExport queryResultExport;

	@Inject
	private CsvQueryResultExporter exporter;

	@Inject
	private UserManager userManager;

	@Inject
	private QueryService queryService;

	private final IModel<CqlQueryResult> queryResultModel;

	private WebMarkupContainer queryErrorDialog;
	
	private IModel<String> queryErrorModel = Model.of("");

	private SwitchableQueryResultPanel queryResultPanel;

	private PageParameters params;

	public QueryEditorPanel(String id, PageParameters params) {
		super(id);
		queryResultModel = Model.of(CqlQueryResult.EMPTY);
		this.params = params;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		setRenderBodyOnly(true);
		cqlHelpPanel = new CqlHelpPanel("cqlHelp");
		add(cqlHelpPanel);

		UserPreferences preferences = userManager.readPreferences();
		cqlCompletionHintPanel = new CompletionHintPanel("cqlInfoHint", "Completion Hint");
		cqlCompletionHintPanel.setVisible(preferences.isShowCqlCompletionHint());
		add(cqlCompletionHintPanel);

		queryResultPanel = new SwitchableQueryResultPanel("queryResultPanel", queryResultModel,
				ViewType.fromOrientation(preferences.getResultOrientation()));
		add(queryResultPanel);
		queryResultPanel.setOutputMarkupPlaceholderTag(true);

		EditorPanel queryEditorPanel = initQueryEditorPanel(params);
		Form<String> editorForm = initForm(queryEditorPanel);
		initButtons(queryEditorPanel, editorForm);

		queryResultExport = new QueryResultExport(this, exporter);

		queryErrorDialog = initQueryErrorDialog();
	}

	private Form<String> initForm(EditorPanel queryEditorPanel) {
		Form<String> form = new Form<>("editorForm");
		form.add(queryEditorPanel);
		add(form);
		return form;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		QueryResultPanel.initQeuryResultJs(response);
	}

	private WebMarkupContainer initQueryErrorDialog() {
		WebMarkupContainer queryErrorDialogRow = new WebMarkupContainer("queryErrorDialogRow");
		queryErrorDialogRow.setVisible(false);
		queryErrorDialogRow.setOutputMarkupPlaceholderTag(true);
		add(queryErrorDialogRow);

		WebMarkupContainer queryErrorDialogCol = new WebMarkupContainer("queryErrorDialogCol");
		queryErrorDialogRow.add(queryErrorDialogCol);

		WebMarkupContainer queryErrorTextPanel = new WebMarkupContainer("queryErrorTextPanel");
		queryErrorDialogCol.add(queryErrorTextPanel);

		Label queryErrorText = new Label("queryErrorText", queryErrorModel);
		queryErrorTextPanel.add(queryErrorText);
		return queryErrorDialogRow;
	}

	private EditorPanel initQueryEditorPanel(PageParameters params) {
		StringValue editorContentVal = params.get("cql");
		String editorContent = editorContentVal == null ? null : editorContentVal.toString();

		EditorPanel queryEditorPanel = new EditorPanel("queryEditorPanel", editorContent);
		add(queryEditorPanel);
		queryEditorPanel.setOutputMarkupPlaceholderTag(true);

		queryEditorPanel.registerCompletionChangeListener(new CompletionChangeHelp());
		queryEditorPanel.registerCompletionChangeListener(new CompletionChangeHint());
		return queryEditorPanel;
	}

	private ButtonsPanel initButtons(final EditorPanel editorPanel, Form<String> editorForm) {
		ButtonsPanel buttonsPanel = new ButtonsPanel("buttons");
		buttonsPanel.withResultOrientation((t, o) -> queryResultPanel.switchView(t, ViewType.fromOrientation(o)));
		buttonsPanel.withCompletion((t, p) -> {
			cqlCompletionHintPanel.setVisible(p);
			t.add(cqlCompletionHintPanel);
		});
		buttonsPanel.withExportQueryResult(t -> queryResultExport.initiateDownload(t, lastQuery));
		buttonsPanel.withExecQuery(t -> handleExecQuery(t, editorPanel), editorForm);
		buttonsPanel.withAddToFavourites();
		add(buttonsPanel);
		return buttonsPanel;
	}

	private void handleExecQuery(AjaxRequestTarget target, EditorPanel editorPanel) {
		// this cannot happen, because java script disables execute
		// button - it's DOS prevention
		if (queryRunning) {
			LOG.warn("Query still running - cannot execute second one");
			return;
		}

		CqlQuery query = editorPanel.getEditorContent();

		if (query == null) {
			return;
		}
		queryRunning = true;
		try {
			CqlQueryResult queryResult = queryService.execute(query);
			lastQuery = query;
			queryResultModel.setObject(queryResult);
			queryResultPanel.modelChanged();
			queryResultPanel.setVisible(true);
			queryErrorDialog.setVisible(false);
		} catch (Exception e) {
			queryErrorDialog.setVisible(true);
			queryResultPanel.setVisible(false);
			queryErrorModel.setObject(e.getMessage());
		} finally {
			queryRunning = false;
		}
		editorPanel.resetCompletion();

		target.add(queryErrorDialog);
		target.add(queryResultPanel);
		QueryResultPanel.appendQeuryResultJs(target);
	}

	private final class CompletionChangeHelp implements CompletionChangeListener {
		@Override
		public void onCompletionChange(ContextCqlCompletion currentCompletion) {
			cqlHelpPanel.changeCompletion(currentCompletion);
		}

		@Override
		public Component getReferencesForRefresh() {
			return cqlHelpPanel;
		}
	}

	private final class CompletionChangeHint implements CompletionChangeListener {
		@Override
		public void onCompletionChange(ContextCqlCompletion currentCompletion) {
			cqlCompletionHintPanel.changeCompletion(currentCompletion);
		}

		@Override
		public Component getReferencesForRefresh() {
			return cqlCompletionHintPanel;
		}
	}

}
