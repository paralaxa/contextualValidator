package sk.stopangin.contextual;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
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

  @Override
  public void initialize(ValidateWhen contextualValidation) {
    expression = expressionParser.parseExpression(contextualValidation.condition());
  }

  @Override
  public boolean isValid(Model model, ConstraintValidatorContext constraintValidatorContext) {
    Field[] declaredFields = model.getClass().getDeclaredFields();

    if (expression.getValue(model, Boolean.class)) {
      for (Field declaredField : declaredFields) {
        List<Annotation> conditionalAnnotation = getConditionalAnnotation(declaredField);
        if (conditionalAnnotation != null) {
          for (Annotation annotation : conditionalAnnotation) {
            String value = getValueSafe(annotation);
            String data = getDataSafe(annotation);
            String expressionStr = value.replaceAll("<#FIELDNAME#>", declaredField.getName());
            expressionStr = expressionStr.replaceAll("<#DATA#>", data);
            Expression expression = expressionParser.parseExpression(expressionStr);
            Boolean value1 = expression.getValue(model, Boolean.class);
            System.out.printf("Validation result of '%s' is : %b%n", declaredField.getName(),
                value1);
          }
        }
      }
    }
    return true;
  }

  private String getValueSafe(Annotation annotation) {
    return extractAnnotationValueSafe(annotation, "value");
  }

  private String getDataSafe(Annotation annotation) {
    try {
      return (String) annotation.getClass().getMethod("data").invoke(annotation);
    } catch (Exception e) {
      return "";
    }

  }

  private String extractAnnotationValueSafe(Annotation annotation, String fieldName) {
    try {
      return (String) annotation.annotationType().getDeclaredField(fieldName).get(annotation);
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

  private static class AnnotationDefinitionHolder {

    public AnnotationDefinitionHolder(Annotation annotation,
        Annotation[] annotationTypeAnnotation) {
      this.annotation = annotation;
      this.annotationTypeAnnotation = annotationTypeAnnotation;
    }

    private Annotation annotation;
    private Annotation[] annotationTypeAnnotation;
  }
}
