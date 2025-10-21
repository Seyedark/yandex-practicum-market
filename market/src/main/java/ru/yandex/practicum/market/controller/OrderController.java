package ru.yandex.practicum.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.enums.OrderStatusEnum;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.OrderService;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CoordinatorService coordinatorService;

    @GetMapping("/cart")
    public Mono<String> getCart(Model model) {
        return orderService.findCartOrderAndCheck()
                .map(orderWithItemsAndResponseDto -> {
                    model.addAttribute("items", orderWithItemsAndResponseDto.getOrderWithItemsDto().getItemList());
                    model.addAttribute("total", orderWithItemsAndResponseDto.getOrderWithItemsDto().getTotalAmount());
                    model.addAttribute("empty", orderWithItemsAndResponseDto.getOrderWithItemsDto().getItemList().isEmpty());
                    if (orderWithItemsAndResponseDto.getBalanceApiResponseDto() != null) {
                        model.addAttribute("code", orderWithItemsAndResponseDto.getBalanceApiResponseDto().getCode());
                        model.addAttribute("errorMessage", orderWithItemsAndResponseDto.getBalanceApiResponseDto().getErrorMessage());
                    }
                    return "cart";
                });
    }

    @PostMapping("/cart/items/{id}")
    public Mono<String> changeItemsInOrder(@PathVariable("id") Long id,
                                           ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    String form = formData.getFirst("form");
                    return coordinatorService.changeItemsInOrder(id, action)
                            .then(Mono.fromSupplier(() -> {
                                if ("main".equals(form)) {
                                    return "redirect:/";
                                } else if ("cart".equals(form)) {
                                    return "redirect:/" + form;
                                } else {
                                    return "redirect:/" + form + "?id=" + id;
                                }
                            }));
                });
    }

    @PostMapping("/buy")
    public Mono<String> buy(Model model) {
        return orderService.closeOrder()
                .map(orderWithItemsAndResponseDto -> {
                    if (orderWithItemsAndResponseDto.getOrderWithItemsDto() != null) {
                        model.addAttribute("items", orderWithItemsAndResponseDto.getOrderWithItemsDto().getItemList());
                        model.addAttribute("order", orderWithItemsAndResponseDto.getOrderWithItemsDto());
                        return "order";
                    } else {
                        return "redirect:/cart";
                    }
                });
    }

    @GetMapping("/orders")
    public Mono<String> getAllOrders(Model model) {
        return orderService.findOrdersWithItemsByStatus(OrderStatusEnum.ORDER.name(), true)
                .collectList()
                .map(orderWithItemsDtoList -> {
                    model.addAttribute("orders", orderWithItemsDtoList);
                    return "orders";
                });
    }

    @GetMapping("/orders/{id}")
    public Mono<String> getClosedOrder(@PathVariable("id") Long id,
                                       Model model) {
        return orderService.findOrdersWithItemsById(id)
                .map(orderWithItemsDto -> {
                    model.addAttribute("order", orderWithItemsDto);
                    return "order";
                });
    }
}