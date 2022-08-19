package sk.stopangin.contextual;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.Data;

public class Test {

  public static void main(String[] args) {
    Validator v = Validation.buildDefaultValidatorFactory().getValidator();
    Model t = new Model();
    t.setWeather("shiny");
    t.setShouldValidate(true);
    t.setGajaka("yyy");
    Test2 t2 = new Test2();
    t.setTest2(t2);
    v.validate(t).forEach(modelConstraintViolation -> {
      System.out.println(
          modelConstraintViolation.getPropertyPath() + " " + modelConstraintViolation.getMessage());
    });

  }

  @ValidateConditionaly(condition = "weather =='shiny'")
  @Data
  public static class Model {

    @Condition("shouldValidate")
    @ConditionalSize(data = "2")
    private List<String> data = new ArrayList<>();
    private String weather;
    private boolean shouldValidate;
    @ConditionalValue(data = "xxx")
    private String gajaka;
    @Condition("gajaka.equals('yyy')")
    @Valid
    private Test2 test2;
  }

  @Data
  @ValidateConditionaly
  public static class Test2 {

    @ConditionalNonEmpty
    private String zajec;
  }
}
