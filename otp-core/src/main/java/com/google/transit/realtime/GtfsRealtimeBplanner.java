// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: gtfs-realtime-bplanner.proto

package com.google.transit.realtime;

public final class GtfsRealtimeBplanner {
  private GtfsRealtimeBplanner() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.deviated);
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.wheelchairAccessible);
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.vehicleType);
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.phoneNumber);
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.driverName);
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.stopDistancePercent);
    registry.add(com.google.transit.realtime.GtfsRealtimeBplanner.blockId);
  }
  public static final int DEVIATED_FIELD_NUMBER = 1061;
  /**
   * <code>extend .transit_realtime.VehicleDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.VehicleDescriptor,
      java.lang.Boolean> deviated = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Boolean.class,
        null);
  public static final int WHEELCHAIRACCESSIBLE_FIELD_NUMBER = 1062;
  /**
   * <code>extend .transit_realtime.VehicleDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.VehicleDescriptor,
      java.lang.Integer> wheelchairAccessible = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Integer.class,
        null);
  public static final int VEHICLE_TYPE_FIELD_NUMBER = 1063;
  /**
   * <code>extend .transit_realtime.VehicleDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.VehicleDescriptor,
      java.lang.Integer> vehicleType = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Integer.class,
        null);
  public static final int PHONE_NUMBER_FIELD_NUMBER = 1064;
  /**
   * <code>extend .transit_realtime.VehicleDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.VehicleDescriptor,
      java.lang.String> phoneNumber = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int DRIVER_NAME_FIELD_NUMBER = 1065;
  /**
   * <code>extend .transit_realtime.VehicleDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.VehicleDescriptor,
      java.lang.String> driverName = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int STOP_DISTANCE_PERCENT_FIELD_NUMBER = 1066;
  /**
   * <code>extend .transit_realtime.VehicleDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.VehicleDescriptor,
      java.lang.Integer> stopDistancePercent = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Integer.class,
        null);
  public static final int BLOCK_ID_FIELD_NUMBER = 1066;
  /**
   * <code>extend .transit_realtime.TripDescriptor { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.transit.realtime.GtfsRealtime.TripDescriptor,
      java.lang.String> blockId = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\034gtfs-realtime-bplanner.proto\022\020transit_" +
      "realtime\032\023gtfs-realtime.proto:=\n\010deviate" +
      "d\022#.transit_realtime.VehicleDescriptor\030\245" +
      "\010 \001(\010:\005false:E\n\024wheelchairAccessible\022#.t" +
      "ransit_realtime.VehicleDescriptor\030\246\010 \001(\005" +
      ":\0010::\n\014vehicle_type\022#.transit_realtime.V" +
      "ehicleDescriptor\030\247\010 \001(\005::\n\014phone_number\022" +
      "#.transit_realtime.VehicleDescriptor\030\250\010 " +
      "\001(\t:9\n\013driver_name\022#.transit_realtime.Ve" +
      "hicleDescriptor\030\251\010 \001(\t:C\n\025stop_distance_",
      "percent\022#.transit_realtime.VehicleDescri" +
      "ptor\030\252\010 \001(\005:3\n\010block_id\022 .transit_realti" +
      "me.TripDescriptor\030\252\010 \001(\tB\035\n\033com.google.t" +
      "ransit.realtime"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          deviated.internalInit(descriptor.getExtensions().get(0));
          wheelchairAccessible.internalInit(descriptor.getExtensions().get(1));
          vehicleType.internalInit(descriptor.getExtensions().get(2));
          phoneNumber.internalInit(descriptor.getExtensions().get(3));
          driverName.internalInit(descriptor.getExtensions().get(4));
          stopDistancePercent.internalInit(descriptor.getExtensions().get(5));
          blockId.internalInit(descriptor.getExtensions().get(6));
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.transit.realtime.GtfsRealtime.getDescriptor(),
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}