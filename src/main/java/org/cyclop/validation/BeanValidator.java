package org.cyclop.validation;

import com.google.common.collect.ImmutableSet;
import org.cyclop.model.exception.BeanValidationException;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.ConstraintDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** @author Maciej Miklas */
public final class BeanValidator {
	private final static String NULL_MARKER = "NULL-" + UUID.randomUUID();

	private final static Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	private Map<String, Object> objMap = new HashMap<>();

	public static BeanValidator create() {
		return new BeanValidator();
	}

	public static BeanValidator create(Object obj) {
		return new BeanValidator().add(obj);
	}

	public BeanValidator add(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("null validation object");
		}
		return add(obj.getClass().getSimpleName(), obj);
	}

	public BeanValidator add(String name, Object obj) {
		if (name == null) {
			throw new IllegalArgumentException("null name for validation object");
		}

		String key = objMap.containsKey(name) ? name + " - " + UUID.randomUUID() : name;
		objMap.put(key, obj == null ? NULL_MARKER : obj);
		return this;
	}

	// TODO add map/set descend validation
	public void validate() {
		Map<String, Set<ConstraintViolation<Object>>> violations = new HashMap<>();

		for (Map.Entry<String, Object> obj : objMap.entrySet()) {
			Object value = obj.getValue();
			if (value.toString().equals(NULL_MARKER)) {
				violations.put(obj.getKey(), createViolationForNullRoot());
				continue;
			}
			Set<ConstraintViolation<Object>> violation = VALIDATOR.validate(value);
			if (!violation.isEmpty()) {
				violations.put(obj.getKey(), violation);
			}
		}

		if (!violations.isEmpty()) {
			throw new BeanValidationException(violations);
		}
	}

	private Set<ConstraintViolation<Object>> createViolationForNullRoot() {
		ConstraintViolation<Object> viol = new NullRootViolation();
		Set<ConstraintViolation<Object>> violSet = ImmutableSet.of(viol);
		return violSet;
	}

	public final class NullRootViolation implements ConstraintViolation<Object> {
		private String message = "NULL_ROOT_OBJECT";

		@Override
		public ConstraintDescriptor<?> getConstraintDescriptor() {
			return null;
		}

		@Override
		public Object getInvalidValue() {
			return null;
		}

		@Override
		public Object getLeafBean() {
			return null;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public String getMessageTemplate() {
			return message;
		}

		@Override
		public Path getPropertyPath() {
			return null;
		}

		@Override
		public Object getRootBean() {
			return null;
		}

		@Override
		public Class<Object> getRootBeanClass() {
			return null;
		}

		@Override
		public String toString() {
			return message;
		}


	}
}
