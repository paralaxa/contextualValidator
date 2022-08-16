package sk.stopangin.contextual;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.Data;

public class Test {

  public static void main(String[] args) {
    Validator v = Validation.buildDefaultValidatorFactory().getValidator();
    Model t = new Model();
    t.setValue("g");
    t.setWeather("shiny");
    t.setShouldValidate(true);
    Set<ConstraintViolation<Model>> validate = v.validate(t);
    System.out.println(validate);
  }

  @ValidateWhen(condition = "weather =='shiny'")
  @Data
  public static class Model {

    @ConditionalSize(data = "2")
    private List<String> data = new ArrayList<>();
    private String weather;
    private boolean shouldValidate;
    @ConditionalNonEmpty
    private String value;
  }
}
