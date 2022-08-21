package sk.stopangin.contextual.cata;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import sk.stopangin.contextual.cata.CmpKafkaProperties.Flow.FlowQueues;
import sk.stopangin.contextual.cata.Kafka07FlowConfigurationDefinition.FlowDescription;

@Data
@NoArgsConstructor
@AllArgsConstructor
@KafkaPropertiesValidation
public class CmpKafkaProperties {

  @Data
  public static class Flow {

    @Data
    public static class FlowQueues {

      private List<FlowDescription> consumer = new ArrayList<>();
      private List<FlowDescription> producer = new ArrayList<>();
    }

    private FlowQueues confluent = new FlowQueues();
    private FlowQueues k7 = new FlowQueues();
  }

  @Data
  public static class Producer {

    private String acks;
    @NotEmpty
    private String idPrefix;
    @NotEmpty
    private String clientIdSuffix;
    private CompressionType compressionType;
    @NotEmpty
    private String topic;
  }

  @Data
  public static class Consumer {

    @NotEmpty
    private String idPrefix;
    @NotEmpty
    private String clientIdSuffix;
    @NotEmpty
    private String groupIdSuffix;
    @NotEmpty
    private String topic;
    private int parallelism;
    private int pollTimeout;
    private int sessionTimeout;
    private Integer maxPollRecords;
    @ValidateIfFlowIsActive
    private Test test;
  }

  private static class Test {

    @NotEmpty
    private String testValue;
  }

  private Flow flow = new Flow();

  // WARNING: keep these in alphabetical order until all of the services have been migrated to SB.
  // This is to create a compile error if another producer/consumer is created, but order must
  // be preserved  Otherwise one Producer/Consumer values will be placed in another queue.
  // See: KafkaPropertiesReader for implementation for non-SB services of reading in properties.
  // The order of constructor parameters is determined by the order of the class members.
  // If we try to use setters, we don't have any guarantee that new fields added here are added
  // in the KafkaPropertiesReader.

  @ValidateIfFlowIsActive
  private Consumer adGroupsConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer adGroupsProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer batchConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer batchProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer entityChangeEventsConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer entityChangeEventsProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer eventsConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer eventsProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer ordersConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer ordersProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer posConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer posProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer promotionsConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer promotionsProducer = new Producer();

  @ValidateIfFlowIsActive
  private Consumer simulationConsumer = new Consumer();
  @ValidateIfFlowIsActive
  private Producer simulationProducer = new Producer();

  private boolean kafkaLibratoMetricsAllowed;


  enum CompressionType {
    NONE, ZIP
  }


  public static void main(String[] args) {
    Validator v = Validation.buildDefaultValidatorFactory().getValidator();
    CmpKafkaProperties t = new CmpKafkaProperties();
    Consumer adGroupsConsumer1 = new Consumer();
    adGroupsConsumer1.setIdPrefix("ad");
    adGroupsConsumer1.setTest(new Test());
    adGroupsConsumer1.setGroupIdSuffix("g");
    t.setPromotionsConsumer(null);
    t.setAdGroupsConsumer(adGroupsConsumer1);
    Flow confluentFlow = new Flow();
    FlowQueues confluent = new FlowQueues();
    ArrayList<FlowDescription> consumer = new ArrayList<>();
    consumer.add(FlowDescription.AD_GROUPS);
    consumer.add(FlowDescription.PROMOTIONS);
    confluent.setConsumer(consumer);
    confluentFlow.setConfluent(confluent);
    t.setFlow(confluentFlow);

    v.validate(t).forEach(cmpKafkaPropertiesConstraintViolation -> System.out.println(
        cmpKafkaPropertiesConstraintViolation.getPropertyPath() + "   "
            + cmpKafkaPropertiesConstraintViolation.getMessage()));

  }
}
