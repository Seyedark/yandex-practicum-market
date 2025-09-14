package ru.yandex.practicum.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.enums.OrderStatusEnum;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.OrderService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CoordinatorService coordinatorService;

    @GetMapping("/cart")
    public String getCart(Model model) {
        OrderEntity orderEntity = orderService.findCartOrder();

        List<ItemEntity> entityList = extractItemsFromOrder(orderEntity);
        model.addAttribute("items", extractItemsFromOrder(orderEntity));
        model.addAttribute("total", orderEntity.getTotalAmount());
        model.addAttribute("empty", entityList.isEmpty());
        return "cart";
    }

    @PostMapping("/cart/items/{id}")
    public String changeItemsInOrder(@PathVariable("id") Long id,
                                     @RequestParam("action") String action,
                                     @RequestParam("form") String form) {
        coordinatorService.changeItemsInOrder(id, action);
        if (form.equals("main")) {
            return "redirect:/";
        } else if (form.equals("cart")) {
            return "redirect:/" + form;
        } else {
            return "redirect:/" + form + "?id=" + id;
        }
    }

    @PostMapping("/buy")
    public String buy(Model model) {
        OrderEntity orderEntity = orderService.closeOrder();

        model.addAttribute("order", orderEntity);
        model.addAttribute("items", extractItemsFromOrder(orderEntity));
        return "order";
    }

    @GetMapping("/orders")
    public String getAllOrders(Model model) {
        List<OrderEntity> orderEntityList = orderService.findOrderByStatus(OrderStatusEnum.ORDER.name());
        model.addAttribute("orders", orderEntityList);
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getClosedOrder(@PathVariable("id") Long id,
                                 Model model) {
        OrderEntity orderEntity = orderService.findById(id);

        model.addAttribute("order", orderEntity);
        model.addAttribute("items", extractItemsFromOrder(orderEntity));
        return "order";
    }

    private List<ItemEntity> extractItemsFromOrder(OrderEntity orderEntity) {
        return orderEntity.getOrderItem()
                .stream()
                .map(orderItem -> {
                    ItemEntity item = orderItem.getItem();
                    item.setQuantity(orderItem.getQuantity());
                    return item;
                })
                .toList();
    }
}