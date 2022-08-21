package sk.stopangin.contextual.cata;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import sk.stopangin.contextual.cata.CmpKafkaProperties.Flow.FlowQueues;
import sk.stopangin.contextual.cata.Kafka07FlowConfigurationDefinition.FlowDescription;
import sk.stopangin.contextual.cata.KafkaPropertiesValidator.KafkaValidationIntrospector.ObjectFieldValidation.ValidatorWithMessage;
import sk.stopangin.contextual.cata.KafkaPropertiesValidator.KafkaValidationIntrospector.ValidationContext;

public class KafkaPropertiesValidator implements
    ConstraintValidator<KafkaPropertiesValidation, CmpKafkaProperties> {

  private KafkaValidationIntrospector kafkaValidationAnnotationIntrospector;

  @Override
  public void initialize(KafkaPropertiesValidation kafkaPropertiesValidation) {

  }

  @Override
  public boolean isValid(CmpKafkaProperties kafkaProperties,
      ConstraintValidatorContext constraintValidatorContext) {
    kafkaValidationAnnotationIntrospector = new KafkaValidationIntrospector(
        new ActiveFlowsHandler(kafkaProperties));
    ValidationContext validationContext = kafkaValidationAnnotationIntrospector.build(
        kafkaProperties);
    return isValid(validationContext, constraintValidatorContext, "");
  }

  private boolean isValid(ValidationContext validationContext,
      ConstraintValidatorContext constraintValidatorContext, String propertyPath) {
    AtomicBoolean isValid = new AtomicBoolean(true);
    validationContext.objectFieldValidations.forEach(objectFieldValidation -> {
      objectFieldValidation.validators.forEach(validatorWithMessage -> {
        if (!validatorWithMessage.validator.test(
            getFieldValueSafe(objectFieldValidation.rootObject,
                objectFieldValidation.validatedField))) {
          isValid.set(false);
          constraintValidatorContext.disableDefaultConstraintViolation();
          constraintValidatorContext.buildConstraintViolationWithTemplate(
                  validatorWithMessage.validationMessage)
              .addPropertyNode(propertyPath)
              .addPropertyNode(objectFieldValidation.rootField.getName())
              .addPropertyNode(objectFieldValidation.validatedField.getName())
              .addConstraintViolation();
        }
      });
    });
    for (ValidationContext childContext : validationContext.childContexts) {
      boolean isChildValid = isValid(childContext, constraintValidatorContext,
          validationContext.contextField == null ? "" : validationContext.contextField.getName());
      if (isValid.get()) {
        isValid.set(isChildValid);
      }
    }
    return isValid.get();
  }

  static Object getFieldValueSafe(Object object, Field field) {
    try {
      field.setAccessible(true);
      return field.get(object);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  private static class ActiveFlowsHandler {

    private final FlowFieldNameToFLowDescriptionConverter flowFieldNameToFLowDescriptionConverter = new FlowFieldNameToFLowDescriptionConverter();
    private final FlowQueues confluent;

    public ActiveFlowsHandler(CmpKafkaProperties kafkaProperties) {
      this.confluent = kafkaProperties.getFlow().getConfluent();
    }

    public boolean isFlowActive(String fieldName) {
      boolean isConsumer = fieldName.toLowerCase(Locale.ROOT).contains("consumer");
      Optional<FlowDescription> flowDescription = flowFieldNameToFLowDescriptionConverter.fromFieldName(
          fieldName);
      List<FlowDescription> flowDescriptions = confluent.getProducer();
      if (isConsumer) {
        flowDescriptions = confluent.getConsumer();
      }
      if (flowDescription.isPresent()) {
        return flowDescriptions.contains(flowDescription.get());
      }
      return false;
    }
  }

  private static class FlowFieldNameToFLowDescriptionConverter {

    private Optional<FlowDescription> fromFieldName(String fieldName) {
      return Arrays.stream(FlowDescription.values())

          .filter(flowDescription -> {
            String strFlowDescription = flowDescription.name()
                .toLowerCase(Locale.ROOT)
                .replaceAll("_", "");
            String fieldNameUpdated = fieldName
                .toLowerCase(Locale.ROOT)
                .replaceAll("consumer", "")
                .replaceAll("producer", "");
            return fieldNameUpdated.equals(strFlowDescription);
          }).findAny();
    }
  }

  static class KafkaValidationIntrospector {

    private final ActiveFlowsHandler activeFlowsHandler;
    private final Map<Class<? extends Annotation>, ValidatorWithMessage> validationAndValidator = new HashMap<>();

    public KafkaValidationIntrospector(ActiveFlowsHandler activeFlowsHandler) {
      this.activeFlowsHandler = activeFlowsHandler;
      validationAndValidator.put(NotEmpty.class,
          new ValidatorWithMessage(
              s -> {
                if (s instanceof String) {
                  return !((String) s).isEmpty();
                } else {
                  return s != null;
                }
              }, "Value cannot be empty"));
    }

    private ValidationContext build(ValidationContext context, boolean validateBranch) {

      enrichWithObjectFieldValidation(context);
      Field[] declaredFields = context.contextField == null ?
          context.contextValue.getClass().getDeclaredFields() :
          context.contextField.getType().getDeclaredFields();
      for (Field declaredField : declaredFields) {
        if (declaredField.getDeclaredAnnotation(ValidateIfFlowIsActive.class) != null &&
            (activeFlowsHandler.isFlowActive(declaredField.getName()) || validateBranch)) {
          ValidationContext childContext = new ValidationContext();
          context.childContexts.add(childContext);
          childContext.contextField = declaredField;
          childContext.contextValue = getFieldValueSafe(context.contextValue, declaredField);
          build(childContext, true);
        }
      }
      return context;
    }

    private void enrichWithObjectFieldValidation(ValidationContext context) {
      Field contextField = context.contextField;
      Object contextValue = context.contextValue;
      context.objectFieldValidations = getValidationObjectDataForRoot(contextField, contextValue);
    }

    ValidationContext build(Object root) {

      ValidationContext context = new ValidationContext();
      context.contextValue = root;
      build(context, false);
      return context;
    }


    public List<ObjectFieldValidation> getValidationObjectDataForRoot(Field rootField,
        Object root) {
      if (root == null) {
        return new ArrayList<>();
      }
      List<ObjectFieldValidation> result = new ArrayList<>();
      Field[] declaredFields = root.getClass().getDeclaredFields();
      for (Field declaredField : declaredFields) {
        Annotation[] declaredAnnotations = declaredField.getDeclaredAnnotations();
        List<ValidatorWithMessage> validators = new ArrayList<>();
        for (Annotation declaredAnnotation : declaredAnnotations) {
          if (validationAndValidator.get(declaredAnnotation.annotationType()) != null) {
            validators.add(validationAndValidator.get(declaredAnnotation.annotationType()));
          }
        }
        if (!validators.isEmpty()) {
          result.add(new ObjectFieldValidation(rootField, root, declaredField, validators));
        }
      }
      return result;
    }


    static class ValidationContext {

      private List<ValidationContext> childContexts = new ArrayList<>();
      private Field contextField;
      private Object contextValue;

      private List<ObjectFieldValidation> objectFieldValidations = new ArrayList<>();


    }

    @AllArgsConstructor
    static class ObjectFieldValidation {

      private Field rootField;
      private Object rootObject;
      private Field validatedField;
      private List<ValidatorWithMessage> validators;

      @AllArgsConstructor
      static class ValidatorWithMessage {

        private Predicate<Object> validator;
        private String validationMessage;

      }
    }
  }
}
