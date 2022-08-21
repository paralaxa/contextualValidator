package sk.stopangin.contextual;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Data;

public class TestFunction {


  private void migrate() {
    doMigrate(this::generateAdgroupMapping);
    doMigrate(this::generatePromotionMapping);
  }

  private void doMigrate(Function<String, ? extends Holder> generateFunction) {
    String data = "x";
    Holder apply = generateFunction.apply(data);
    saveMapping(apply);
  }

  private void saveMapping(Holder mapping) {
    List<Holder> holders = new ArrayList<>();
    holders.add(mapping);
    System.out.println(mapping.getCmpId());
  }

  private AdgroupHolder generateAdgroupMapping(String from) {
    return new AdgroupHolder();
  }

  private PromotionHolder generatePromotionMapping(String from) {
    return new PromotionHolder();
  }

  @Data
  private static class PromotionHolder implements Holder {

    private String cmpId;
  }

  @Data
  private static class AdgroupHolder implements Holder {

    private String cmpId;
  }

  private interface Holder {

    String getCmpId();
  }
}
