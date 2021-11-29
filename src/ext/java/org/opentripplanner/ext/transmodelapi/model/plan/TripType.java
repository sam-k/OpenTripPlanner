package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.util.ResourceBundleSingleton;

import java.util.stream.Collectors;

public class TripType {
  public static GraphQLObjectType create(
      GraphQLObjectType placeType,
      GraphQLObjectType tripPatternType,
      GraphQLObjectType tripMetadataType,
      GraphQLObjectType routingErrorType,
      GqlUtil gqlUtil

  ) {
    return GraphQLObjectType.newObject()
        .name("Trip")
        .description("Description of a travel between two places.")
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("dateTime")
            .description("The time and date of travel")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.date.getTime())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("metadata")
            .description("The trip request metadata.")
            .deprecate("Use pageCursor instead")
            .type(tripMetadataType)
            .dataFetcher(env -> ((PlanResponse) env.getSource()).metadata)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("fromPlace")
            .description("The origin")
            .type(new GraphQLNonNull(placeType))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.from)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("toPlace")
            .description("The destination")
            .type(new GraphQLNonNull(placeType))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.to)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("tripPatterns")
            .description("A list of possible trip patterns")
            .type(new GraphQLNonNull(new GraphQLList(tripPatternType)))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.itineraries)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("messageEnums")
            .description("A list of possible error messages as enum")
            .deprecate("Use routingErrors instead")
            .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).messages.stream()
                    .map(routingError -> PlannerErrorMapper.mapMessage(routingError).message)
                    .map(Enum::name)
                    .collect(Collectors.toList()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("messageStrings")
            .deprecate("Use routingErrors instead")
            .description("A list of possible error messages in cleartext")
            .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).messages.stream()
                    .map(routingError -> PlannerErrorMapper.mapMessage(routingError).message)
                    .map(message -> message.get(ResourceBundleSingleton.INSTANCE.getLocale(env.getArgument("locale"))))
                    .collect(Collectors.toList())
            )
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("routingErrors")
            .description("A list of routing errors, and fields which caused them")
            .type(new GraphQLNonNull(new GraphQLList(routingErrorType)))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).messages)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("debugOutput")
            .description("Information about the timings for the trip generation")
            .type(new GraphQLNonNull(GraphQLObjectType.newObject()
                .name("debugOutput")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("totalTime")
                    .type(ExtendedScalars.GraphQLLong)
                    .build())
                .build()))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).debugOutput)
            .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("pageCursor")
                .description("Use the cursor to get the next page of results. Use this cursor for "
                    + "the pageCursor parameter in the trip query in order to get the next page.\n"
                    + "If arriveBy=false the next page is a set of itineraries departing AFTER the "
                    + "last itinerary in this result.\n"
                    + "If arriveBy=true the next page is a set of itineraries departing BEFORE the "
                    + "first itinerary in this result.")
                .type(Scalars.GraphQLString)
                .dataFetcher(env -> ((PlanResponse) env.getSource()).pageCursor.encode())
                .build()
            )
        .build();
  }
}
