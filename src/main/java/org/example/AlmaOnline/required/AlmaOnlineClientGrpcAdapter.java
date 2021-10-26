package org.example.AlmaOnline.required;

import com.google.common.util.concurrent.ListenableFuture;
import org.example.AlmaOnline.provided.client.*;
import org.example.AlmaOnline.server.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// AlmaOnlineClientGrpcAdapter provides your own implementation of the AlmaOnlineClientAdapter
public class AlmaOnlineClientGrpcAdapter implements AlmaOnlineClientAdapter {
    // getRestaurants should retrieve the information on all the available restaurants.
    @Override
    public List<RestaurantInfo> getRestaurants(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub) {
        RestaurantsReply reply = stub.restaurants(RestaurantsRequest.newBuilder().build());

        List<RestaurantInfo> restaurantInfos = new ArrayList<>();
        for (var rpcRestaurant : reply.getRestaurantsList()) {
            restaurantInfos.add(new RestaurantInfo(
                    rpcRestaurant.getId(),
                    rpcRestaurant.getName()
            ));
        }
        return restaurantInfos;
    }

    // getMenu should return the menu of a given restaurant
    @Override
    public MenuInfo getMenu(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId) {
        MenuReply reply = stub.menu(MenuRequest.newBuilder().setRid(restaurantId).build());
        Map<String, Double> items = new HashMap<>();
        for(var rpcItem: reply.getMenu().getItemsList()) {
            items.put(rpcItem.getName(), rpcItem.getPrice());
        }
        return new MenuInfo(items);
    }

    // createDineInOrder should create the given dine-in order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDineInOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DineInOrderQuote order) {
        return stub.createDineInOrder(
                CreateDineInOrderRequest.newBuilder()
                        .setDineInOrder(
                                RpcOrder.RpcDineInOrder.newBuilder()
                                        .setOid(order.getOrderId())
                                        .setRid(order.getRestaurantId())
                                        .setCustomer(order.getCustomer())
                                        .setCreateDate((new java.util.Date(1000 * (new Date().getTime() / 1000)).toString())
)                                       .addAllItems(order.getItems()
                                                .stream()
                                                .map(item -> RpcMenuItem.newBuilder().setName(item).build())
                                                .collect(Collectors.toList())
                                        )
                                        .setReservationDate(order.getReservationDate().toString())
                                        .build()
                        ).build()
        );
    }

    // createDeliveryOrder should create the given delivery order at the AlmaOnline server
    @Override
    public ListenableFuture<?> createDeliveryOrder(AlmaOnlineGrpc.AlmaOnlineFutureStub stub, DeliveryOrder order) {
        return stub.createDeliveryOrder(
                CreateDeliveryOrderRequest.newBuilder()
                        .setDeliveryOrder(
                                RpcOrder.RpcDeliveryOrder.newBuilder()
                                        .setOid(order.getOrderId())
                                        .setRid(order.getRestaurantId())
                                        .setCustomer(order.getCustomer())
                                        .setCreateDate((new java.util.Date(1000 * (new Date().getTime() / 1000)).toString())
                                        )                                       .addAllItems(order.getItems()
                                                .stream()
                                                .map(item -> RpcMenuItem.newBuilder().setName(item).build())
                                                .collect(Collectors.toList())
                                        )
                                        .setDeliveryAddress(order.getDeliveryAddress())
                                        .build()
                        ).build()
        );
    }

    // getOrder should retrieve the order information at the AlmaOnline server given the restaurant the order is
    // placed at and the id of the order.
    @Override
    public BaseOrderInfo getOrder(AlmaOnlineGrpc.AlmaOnlineBlockingStub stub, String restaurantId, String orderId) {
        OrderReply reply = stub.getOrder(OrderRequest.newBuilder().setRid(restaurantId).setOid(orderId).build());
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd hh:mm:ss z y", Locale.ENGLISH);
        try {
            if (!reply.getOrder().getDeliveryAddress().equals("None")) {
                return new DeliveryOrderInfo(
                        reply.getOrder().getCustomer(),
                        formatter.parse(reply.getOrder().getCreateDate()),
                        reply.getOrder().getItemsList().stream().map(
                                item -> new ItemInfo(item.getName(), item.getPrice())
                        ).collect(Collectors.toList()),
                        reply.getOrder().getDeliveryAddress());
            } else {
                return new DineInOrderInfo(
                        reply.getOrder().getCustomer(),
                        formatter.parse(reply.getOrder().getCreateDate()),
                        reply.getOrder().getItemsList().stream().map(
                                item -> new ItemInfo(item.getName(), item.getPrice())
                        ).collect(Collectors.toList()),
                        formatter.parse(reply.getOrder().getReservationDate()));
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    // getScript returns the script the application will run during testing.
    // You can leave the default implementation, as it will test most of the functionality.
    // Alternatively, you can provide your own implementation to test your own edge-cases.
    @Override
    public AppScript getScript() {
        return AlmaOnlineClientAdapter.super.getScript();
    }
}
