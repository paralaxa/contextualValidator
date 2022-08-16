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
import sk.stopangin.contextual.Test.Model;

public class ContextualValidator implements
    ConstraintValidator<ValidateWhen, Test.Model> {

  private final ExpressionParser expressionParser = new SpelExpressionParser();
  private Expression expression;
  private final Map<Field, Expression> fieldExpressionMap = new ConcurrentHashMap<>();
  private boolean initialized;

  @Override
  public void initialize(ValidateWhen contextualValidation) {
    expression = expressionParser.parseExpression(contextualValidation.condition());
  }

  @Override
  public boolean isValid(Model model, ConstraintValidatorContext constraintValidatorContext) {
    AtomicBoolean isValid = new AtomicBoolean(true);
    if (expression.getValue(model, Boolean.class)) {
      if (initialized) {
        fieldExpressionMap.forEach((field, expression) -> {
          if (!expression.getValue(model, Boolean.class)) {
            buildValidationContext(constraintValidatorContext, field);
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

  private void initalizeAndValidate(Model model,
      ConstraintValidatorContext constraintValidatorContext,
      AtomicBoolean isValid) {
    Field[] declaredFields = model.getClass().getDeclaredFields();
    for (Field declaredField : declaredFields) {
      List<Annotation> conditionalAnnotation = getConditionalAnnotation(declaredField);
      if (conditionalAnnotation != null) {
        for (Annotation annotation : conditionalAnnotation) {
          String value = getValueSafe(annotation);
          String data = getDataSafe(annotation);
          String expressionStr = value.replaceAll("<#FIELDNAME#>", declaredField.getName());
          expressionStr = expressionStr.replaceAll("<#DATA#>", data);
          Expression expression = expressionParser.parseExpression(expressionStr);
          fieldExpressionMap.put(declaredField, expression);
          if (!expression.getValue(model, Boolean.class)) {
            buildValidationContext(constraintValidatorContext, declaredField);
            isValid.set(false);
          }

        }
      }
    }
  }

  private void buildValidationContext(ConstraintValidatorContext constraintValidatorContext,
      Field field) {
    constraintValidatorContext
        .buildConstraintViolationWithTemplate("Value is not valid")
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

  private record AnnotationDefinitionHolder(Annotation annotation,
                                            Annotation[] annotationTypeAnnotation) {

  }
}
