package org.cyclop.model;

import net.jcip.annotations.Immutable;

/** @author Maciej Miklas */
@Immutable
public class CqlKeywordValue extends CqlKeyword {

	public static enum Def {
		CLASS("class"), SIMPLE_STRATEGY("simplestrategy"), REPLICATION_FACTOR("replication_factor"),
		NETWORK_TOPOLOGY_STRATEGY(
				"networktopologystrategy"), DURABLE_WRITES("durable_writes"), TRUE("true"), FALSE("false"),
		OLD_NETWORK_TOPOLOGY_STRATEGY(
				"OldNetworkTopologyStrategy");

		private Def(String value) {
			this.value = new CqlKeywordValue(value.toLowerCase());
		}

		public CqlKeywordValue value;
	}

	protected CqlKeywordValue(String val) {
		super(val);
	}

	@Override
	public String toString() {
		return "CqlKeywordValue{" + "part='" + part + '\'' + '}';
	}

	@Override
	public CqlType type() {
		return CqlType.KEYWORD_VALUE;
	}
}
