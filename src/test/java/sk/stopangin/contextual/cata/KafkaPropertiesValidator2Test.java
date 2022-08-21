package sk.stopangin.contextual.cata;

import java.util.ArrayList;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.Assert;
import org.junit.Test;
import sk.stopangin.contextual.cata.CmpKafkaProperties.Consumer;
import sk.stopangin.contextual.cata.CmpKafkaProperties.Flow;
import sk.stopangin.contextual.cata.CmpKafkaProperties.Flow.FlowQueues;
import sk.stopangin.contextual.cata.Kafka07FlowConfigurationDefinition.FlowDescription;

public class KafkaPropertiesValidator2Test {

  KafkaPropertiesValidator2 kafkaPropertiesValidator2 = new KafkaPropertiesValidator2();

  @Test
  public void ifThereAreNoFLowsActiveWeWontValidate() {
    Validator v = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<CmpKafkaProperties>> result = v.validate(new CmpKafkaProperties());
    Assert.assertEquals(0, result.size());
  }

  @Test
  public void ifThereIsFlowActiveWeValidate() {
    Validator v = Validation.buildDefaultValidatorFactory().getValidator();
    CmpKafkaProperties cmpKafkaProperties = new CmpKafkaProperties();
    Consumer adGroupsConsumer1 = new Consumer();
    adGroupsConsumer1.setIdPrefix("ad");
    adGroupsConsumer1.setGroupIdSuffix("g");
    cmpKafkaProperties.setPromotionsConsumer(null);
    cmpKafkaProperties.setAdGroupsConsumer(adGroupsConsumer1);
    Flow confluentFlow = new Flow();
    FlowQueues confluent = new FlowQueues();
    ArrayList<FlowDescription> consumer = new ArrayList<>();
    consumer.add(FlowDescription.AD_GROUPS);
    confluent.setConsumer(consumer);
    confluentFlow.setConfluent(confluent);
    cmpKafkaProperties.setFlow(confluentFlow);
    Set<ConstraintViolation<CmpKafkaProperties>> result = v.validate(cmpKafkaProperties);

    Assert.assertEquals(2, result.size());
  }

  @Test
  public void ifThereIsFlowActiveAndNothingIsFilledWeValidate() {
    Validator v = Validation.buildDefaultValidatorFactory().getValidator();
    CmpKafkaProperties cmpKafkaProperties = new CmpKafkaProperties();
    Flow confluentFlow = new Flow();
    FlowQueues confluent = new FlowQueues();
    ArrayList<FlowDescription> consumer = new ArrayList<>();
    consumer.add(FlowDescription.AD_GROUPS);
    confluent.setConsumer(consumer);
    confluentFlow.setConfluent(confluent);
    cmpKafkaProperties.setFlow(confluentFlow);
    Set<ConstraintViolation<CmpKafkaProperties>> result = v.validate(cmpKafkaProperties);

    Assert.assertEquals(1, result.size());
  }
}