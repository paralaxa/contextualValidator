package sk.stopangin.contextual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalValidation(message = "<#FIELDNAME#>'s size has to be gte <#DATA#>")
public @interface ConditionalSize {

  String value = "<#FIELDNAME#>!=null && <#FIELDNAME#>.size()>= <#DATA#>";

  String data();
}
