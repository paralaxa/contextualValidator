package sk.stopangin.contextual;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class ContextualValidator implements
    ConstraintValidator<ValidateConditionaly, Object> {

  private final ExpressionParser expressionParser = new SpelExpressionParser();
  private Expression expression;
  private final Map<Field, ValidationDataHolder> conditionAndExpressionHolderMap = new ConcurrentHashMap<>();
  private boolean initialized;

  @Override
  public void initialize(ValidateConditionaly contextualValidation) {
    expression = expressionParser.parseExpression(contextualValidation.condition());
  }

  @Override
  public boolean isValid(Object model, ConstraintValidatorContext constraintValidatorContext) {
    AtomicBoolean isValid = new AtomicBoolean(true);
    if (expression.getValue(model, Boolean.class)) {
      if (initialized) {
        conditionAndExpressionHolderMap.forEach((field, conditionAndExpressionHolder) -> {
          Expression condition = conditionAndExpressionHolder.condition();
          Expression expression = conditionAndExpressionHolder.expression();
          String message = conditionAndExpressionHolder.message();
          if (condition.getValue(model, Boolean.class) &&
              !expression.getValue(model, Boolean.class)) {
            buildValidationContext(constraintValidatorContext, field, message);
            isValid.set(false);
          }
        });
      } else {
        initalizeAndValidate(model, constraintValidatorContext, isValid);
        initialized = true;
      }
    }
    return isValid.get();
  }

  private void initalizeAndValidate(Object model,
      ConstraintValidatorContext constraintValidatorContext,
      AtomicBoolean isValid) {
    Field[] declaredFields = model.getClass().getDeclaredFields();
    for (Field declaredField : declaredFields) {
      List<Annotation> conditionalAnnotation = getConditionalAnnotation(declaredField);
      if (conditionalAnnotation != null) {
        for (Annotation annotation : conditionalAnnotation) {
          String value = getValueSafe(annotation);
          String data = getDataSafe(annotation);
          String message = getValidationMessage(annotation);
          String expressionStr = value.replaceAll("<#FIELDNAME#>", declaredField.getName());
          expressionStr = expressionStr.replaceAll("<#DATA#>", data);
          message = message
              .replaceAll("<#FIELDNAME#>", declaredField.getName())
              .replaceAll("<#DATA#>", data);
          Expression expression = expressionParser.parseExpression(expressionStr);
          Expression condition = getCondition(declaredField);
          conditionAndExpressionHolderMap.put(declaredField,
              new ValidationDataHolder(condition, expression, message));
          if (condition.getValue(model, Boolean.class) &&
              !expression.getValue(model, Boolean.class)) {
            buildValidationContext(constraintValidatorContext, declaredField, message);
            isValid.set(false);
          }
        }
      }
    }
  }

  private Expression getCondition(Field field) {
    Condition conditionDefinition = field.getAnnotation(Condition.class);
    if (conditionDefinition != null) {
      return expressionParser.parseExpression(conditionDefinition.value());
    }
    return expressionParser.parseExpression("true");
  }

  private void buildValidationContext(ConstraintValidatorContext constraintValidatorContext,
      Field field, String validationMessage) {
    constraintValidatorContext.disableDefaultConstraintViolation();
    constraintValidatorContext
        .buildConstraintViolationWithTemplate(validationMessage)
        .addPropertyNode(field.getName())
        .addConstraintViolation();
  }

  private String getValueSafe(Annotation annotation) {
    try {
      return (String) annotation.annotationType().getDeclaredField("value").get(annotation);
    } catch (Exception e) {
      return "";
    }
  }

  private String getDataSafe(Annotation annotation) {
    try {
      return (String) annotation.getClass().getMethod("data").invoke(annotation);
    } catch (Exception e) {
      return "";
    }

  }

  private List<Annotation> getConditionalAnnotation(Field field) {
    return Arrays.stream(field.getAnnotations())
        .map(annotation -> new AnnotationDefinitionHolder(annotation,
            annotation.annotationType().getAnnotations()))
        .filter(annotationDefinitionHolder -> Arrays.stream(
                annotationDefinitionHolder.annotationTypeAnnotation)
            .anyMatch(annotation -> annotation instanceof ConditionalValidation))
        .map(annotationDefinitionHolder -> annotationDefinitionHolder.annotation)
        .collect(Collectors.toList());
  }

  private String getValidationMessage(Annotation annotation) {
    return annotation.annotationType()
        .getDeclaredAnnotation(ConditionalValidation.class).message();
  }

  private record AnnotationDefinitionHolder(Annotation annotation,
                                            Annotation[] annotationTypeAnnotation) {

  }

  private record ValidationDataHolder(Expression condition, Expression expression, String message) {

  }
}
