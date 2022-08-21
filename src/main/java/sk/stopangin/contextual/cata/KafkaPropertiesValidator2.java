package sk.stopangin.contextual.cata;


import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import sk.stopangin.contextual.cata.CmpKafkaProperties.Flow.FlowQueues;
import sk.stopangin.contextual.cata.Kafka07FlowConfigurationDefinition.FlowDescription;

public class KafkaPropertiesValidator2 implements
    ConstraintValidator<KafkaPropertiesValidation, CmpKafkaProperties> {

  private final Map<FlowDescription, ObjectAndFieldName> flowDescriptionConsumerMap = new ConcurrentHashMap<>();
  private final Map<FlowDescription, ObjectAndFieldName> flowDescriptionProducerMap = new ConcurrentHashMap<>();
  private boolean initialized;

  @Override
  public boolean isValid(CmpKafkaProperties cmpKafkaProperties,
      ConstraintValidatorContext constraintValidatorContext) {
    if (!initialized) {
      initialize(cmpKafkaProperties);
    }
    FlowQueues confluent = cmpKafkaProperties.getFlow().getConfluent();

    boolean isConsumerValid = isValid(confluent::getConsumer, flowDescriptionConsumerMap::get,
        constraintValidatorContext);
    boolean isProducerValid = isValid(confluent::getProducer, flowDescriptionProducerMap::get,
        constraintValidatorContext);

    return isConsumerValid && isProducerValid;
  }


  private boolean isValid(Supplier<List<FlowDescription>> descriptionsSupplier,
      Function<FlowDescription, ObjectAndFieldName> objectAndFieldNameProvider,
      ConstraintValidatorContext constraintValidatorContext) {
    AtomicBoolean isValid = new AtomicBoolean(true);

    descriptionsSupplier.get().forEach(flowDescription -> {
      ObjectAndFieldName objectAndFieldName = objectAndFieldNameProvider.apply(flowDescription);
      boolean objectValid = isObjectValid(constraintValidatorContext, flowDescription,
          objectAndFieldName);
      if (!objectValid) {
        isValid.set(false);
      }
    });
    return isValid.get();
  }

  private boolean isObjectValid(ConstraintValidatorContext constraintValidatorContext,
      FlowDescription flowDescription, ObjectAndFieldName objectAndFieldName) {
    Object object = objectAndFieldName.object;
    AtomicBoolean isValid = new AtomicBoolean(true);
    if (object == null) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate(
              String.format("cannot be null, as the flow: %s is active",
                  flowDescription.name()))
          .addPropertyNode(objectAndFieldName.fieldName)
          .addConstraintViolation();
      return false;
    } else {
      getValidatedFields(object).stream()
          .filter(field -> isEmpty(getValueSafe(field, object)))
          .forEach(field -> {
            constraintValidatorContext.disableDefaultConstraintViolation();

            constraintValidatorContext
                .buildConstraintViolationWithTemplate("Cannot be empty")
                .addPropertyNode(objectAndFieldName.fieldName)
                .addPropertyNode(field.getName())
                .addConstraintViolation();
            isValid.set(false);
          });
    }
    return isValid.get();
  }

  private boolean isEmpty(Object o) {
    if (o instanceof String) {
      return ((String) o).isEmpty();
    }
    return o == null;
  }

  private Object getValueSafe(Field field, Object objectWithField) {
    try {
      field.setAccessible(true);
      return field.get(objectWithField);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  private List<Field> getValidatedFields(Object object) {
    return Arrays.stream(object.getClass().getDeclaredFields())
        .filter(field -> field.getDeclaredAnnotation(NotEmpty.class) != null)
        .collect(Collectors.toList());
  }

  private void initialize(CmpKafkaProperties cmpKafkaProperties) {
    flowDescriptionConsumerMap.put(FlowDescription.AD_GROUPS,
        new ObjectAndFieldName(cmpKafkaProperties.getAdGroupsConsumer(), "adGroupsConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.AD_GROUPS,
        new ObjectAndFieldName(cmpKafkaProperties.getAdGroupsProducer(), "adGroupsProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.BATCH_REQUESTS,
        new ObjectAndFieldName(cmpKafkaProperties.getBatchConsumer(), "batchConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.BATCH_REQUESTS,
        new ObjectAndFieldName(cmpKafkaProperties.getBatchProducer(), "batchProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.ENTITY_CHANGE_EVENTS,
        new ObjectAndFieldName(cmpKafkaProperties.getEntityChangeEventsConsumer(),
            "entityChangeEventsConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.ENTITY_CHANGE_EVENTS,
        new ObjectAndFieldName(cmpKafkaProperties.getEntityChangeEventsProducer(),
            "entityChangeEventsProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.EVENTS,
        new ObjectAndFieldName(cmpKafkaProperties.getEventsConsumer(), "eventsConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.EVENTS,
        new ObjectAndFieldName(cmpKafkaProperties.getEventsProducer(), "eventsProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.ORDERS,
        new ObjectAndFieldName(cmpKafkaProperties.getOrdersConsumer(), "ordersConsumer"));
    flowDescriptionProducerMap.put(FlowDescription.ORDERS,
        new ObjectAndFieldName(cmpKafkaProperties.getOrdersProducer(), "ordersProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.POS,
        new ObjectAndFieldName(cmpKafkaProperties.getPosConsumer(), "posConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.POS,
        new ObjectAndFieldName(cmpKafkaProperties.getPosProducer(), "posProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.PROMOTIONS,
        new ObjectAndFieldName(cmpKafkaProperties.getPromotionsConsumer(), "promotionsConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.PROMOTIONS,
        new ObjectAndFieldName(cmpKafkaProperties.getPromotionsProducer(), "promotionsProducer"));

    flowDescriptionConsumerMap.put(FlowDescription.SIMULATIONS,
        new ObjectAndFieldName(cmpKafkaProperties.getSimulationConsumer(),
            "simulationsConsumer"));

    flowDescriptionProducerMap.put(FlowDescription.SIMULATIONS,
        new ObjectAndFieldName(cmpKafkaProperties.getSimulationProducer(),
            "simulationsProducer"));
  }

  @AllArgsConstructor
  private static class ObjectAndFieldName {

    private Object object;
    private String fieldName;

  }

  @Override
  public void initialize(KafkaPropertiesValidation kafkaPropertiesValidation) {

  }

}
