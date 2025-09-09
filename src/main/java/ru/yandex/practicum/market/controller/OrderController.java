package ru.yandex.practicum.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.service.OrderService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @RequestMapping("/cart")
    public String getAllItemsByConditions(Model model) {
        OrderEntity orderEntity = orderService.findCartOrder();
        List<ItemEntity> cartItemEntityList = orderService.findItemsFromCart(orderEntity);
        model.addAttribute("items", cartItemEntityList);
        model.addAttribute("total", orderEntity.getTotalAmount());
        model.addAttribute("empty", cartItemEntityList.isEmpty());
        return "cart";
    }

    @GetMapping
    @RequestMapping("/cart/items/{id}")
    public String getAllItemsByConditions(@PathVariable("id") Long id,
                                          @RequestParam("action") String action,
                                          @RequestParam("form") String form) {
        orderService.changeItemsInOrder(id, action);
        if (form.equals("main")) {
            return "redirect:/";
        } else {
            return "redirect:/cart";
        }
    }
}
