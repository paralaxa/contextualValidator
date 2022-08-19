package sk.stopangin.contextual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalValidation(message = "<#FIELDNAME#>'s value has to be: '<#DATA#>'")
public @interface ConditionalValue {

  String value = "('<#DATA#>').equals(<#FIELDNAME#>)";

  String data();
}
