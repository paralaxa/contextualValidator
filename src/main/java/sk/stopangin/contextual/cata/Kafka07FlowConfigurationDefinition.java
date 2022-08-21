package sk.stopangin.contextual.cata;

import javax.validation.Valid;
import lombok.Data;

@Data
public class Kafka07FlowConfigurationDefinition {

  @Valid
  private final CmpKafkaProperties properties;


  public enum FlowDescription {
    ACCOUNTS,
    AD_GROUPS,
    AD_UNITS,
    AUDIENCES,
    BATCH_REQUESTS,
    BILLING,
    CAMPAIGNS,
    CHANGESETS,
    CLEARING_HOUSES,
    CLIENTS,
    CUSTOM_PARAMETER_SPECIFICATION,
    DATA_CATALOGS,
    ENTITY_CHANGE_EVENTS,
    EVENTS,
    EVENTS_CEDW4,
    EXTENSIONS,
    HUB_EVENTS,
    HUB_ACCOUNTS,
    HUB_TOPICS,
    IMAGES,
    LANES,
    LISTS,
    LOCATION_POLYGONS,
    OFFER_CODES,
    ORDERS,
    ORDERS_BACKUP,
    PINS,
    PLACEMENTS,
    POS,
    POS_CEDW4,
    PROMOTIONS,
    SIMULATIONS,
    TEMPLATES,
    STAGED_PROMOTIONS
  }


}
