package sk.stopangin.contextual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalValidation(message = "<#FIELDNAME#> cannot be empty")
public @interface ConditionalNonEmpty {

  String value = "<#FIELDNAME#>!=null && <#FIELDNAME#>!=''";
}
