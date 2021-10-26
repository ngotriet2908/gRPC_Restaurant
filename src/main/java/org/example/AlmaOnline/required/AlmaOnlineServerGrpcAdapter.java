package org.example.AlmaOnline.required;


import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.example.AlmaOnline.defaults.Initializer;
import org.example.AlmaOnline.provided.client.AlmaOnlineClientAdapter;
import org.example.AlmaOnline.provided.client.MenuInfo;
import org.example.AlmaOnline.provided.server.AlmaOnlineServerAdapter;
import org.example.AlmaOnline.provided.service.*;
import org.example.AlmaOnline.server.*;
import org.example.AlmaOnline.provided.service.exceptions.OrderException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

// AlmaOnlineServerGrpcAdapter implements the grpc-server side of the application.
// The implementation should not contain any additional business logic, only implement
// the code here that is required to couple your IDL definitions to the provided business logic.
public class AlmaOnlineServerGrpcAdapter extends AlmaOnlineGrpc.AlmaOnlineImplBase implements AlmaOnlineServerAdapter {

    // the service field contains the AlmaOnline service that the server will
    // call during testing.
    private final AlmaOnlineService service;

    public AlmaOnlineServerGrpcAdapter() {
        this.service = this.getInitializer().initialize();
    }

    // -- Put the code for your implementation down below -- //


    @Override
    public void restaurants(RestaurantsRequest request, StreamObserver<RestaurantsReply> responseObserver) {
        RestaurantsReply reply = RestaurantsReply.newBuilder()
                .addAllRestaurants(
                        service.getRestaurants()
                                .stream()
                                .map(restaurant ->
                                        RpcRestaurant.newBuilder()
                                                .setId(restaurant.getId())
                                                .setName(restaurant.getName())
                                                .build()
                                ).collect(Collectors.toList())
                ).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void menu(MenuRequest request, StreamObserver<MenuReply> responseObserver) {
        MenuReply reply = MenuReply.newBuilder()
                .setMenu(
                        RpcMenuInfo.newBuilder().addAllItems(
                                service.getRestaurant(request.getRid())
                                        .orElseThrow()
                                        .getMenu().getItems()
                                        .stream()
                                        .map(item -> RpcMenuItem.newBuilder()
                                                .setName(item.getName())
                                                .setPrice(item.getPrice())
                                                .build()
                                        ).collect(Collectors.toList()))
                ).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void createDineInOrder(CreateDineInOrderRequest request, StreamObserver<CreateDineInOrderReply> responseObserver) {
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd hh:mm:ss z y", Locale.ENGLISH);

        try {
            service.createDineInOrder(
                    request.getDineInOrder().getRid(),
                    new DineInOrderQuote(
                            request.getDineInOrder().getOid(),
                            formatter.parse(request.getDineInOrder().getCreateDate()),
                            request.getDineInOrder().getCustomer(),
                            request.getDineInOrder().getItemsList()
                                    .stream()
                                    .map(RpcMenuItem::getName)
                                    .collect(Collectors.toList()),
                            formatter.parse(request.getDineInOrder().getReservationDate())
                    )
            );

            System.out.println("order " + request.getDineInOrder().getOid() + " created");
            CreateDineInOrderReply reply = CreateDineInOrderReply.newBuilder()
                    .setMessage("Ok").build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        } catch (OrderException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createDeliveryOrder(CreateDeliveryOrderRequest request, StreamObserver<CreateDeliveryOrderReply> responseObserver) {
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd hh:mm:ss z y", Locale.ENGLISH);

        try {
            service.createDeliveryOrder(
                    request.getDeliveryOrder().getRid(),
                    new DeliveryOrderQuote(
                            request.getDeliveryOrder().getOid(),
                            formatter.parse(request.getDeliveryOrder().getCreateDate()),
                            request.getDeliveryOrder().getCustomer(),
                            request.getDeliveryOrder().getItemsList()
                                    .stream()
                                    .map(RpcMenuItem::getName)
                                    .collect(Collectors.toList()),
                            request.getDeliveryOrder().getDeliveryAddress()
                    )
            );
            CreateDeliveryOrderReply reply = CreateDeliveryOrderReply.newBuilder()
                    .setMessage("Ok").build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        } catch (OrderException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getOrder(OrderRequest request, StreamObserver<OrderReply> responseObserver) {
        var order = service.getOrder(request.getRid(), request.getOid()).orElseThrow();
        OrderReply reply = OrderReply.newBuilder()
                .setOrder(RpcBasicOrder.newBuilder()
                        .setCustomer(order.getCustomer())
                        .setCreateDate(order.getCreationDate().toString())
                        .setDeliveryAddress((order instanceof DeliveryOrder)? ((DeliveryOrder) order).getDeliveryAddress() : "None")
                        .setReservationDate((order instanceof DineInOrder)? ((DineInOrder) order).getReservationDate().toString() : "None")
                        .addAllItems(order.getItems()
                                .stream()
                                .map(item ->
                                        RpcMenuItem.newBuilder()
                                                .setName(item.getName())
                                                .setPrice(item.getPrice())
                                                .build())
                                .collect(Collectors.toList()))
                        .build())
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
