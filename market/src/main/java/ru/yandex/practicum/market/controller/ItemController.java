package ru.yandex.practicum.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.PageResponseDto;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.service.OrderService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final OrderService orderService;

    @GetMapping("/")
    public Mono<String> getAllItemsByConditions(@RequestParam(name = "search", required = false) String search,
                                                @RequestParam(name = "sortBy", defaultValue = "NO") String sort,
                                                @RequestParam(name = "page", defaultValue = "1") long page,
                                                @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                                Model model) {
        Mono<PageResponseDto> pageResponseDtoMono = itemService.getAllItemsByConditions(orderService.findCartOrder(false), search, sort, page, pageSize);
        return pageResponseDtoMono.flatMap(pageData -> {
            model.addAttribute("items", pageData.getItemDtoList());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pageData.getTotalPages() == 0 ? 1 : pageData.getTotalPages());
            model.addAttribute("currentSize", pageSize);
            model.addAttribute("sort", sort);
            return Mono.just("main");
        });
    }

    @GetMapping("/item")
    public Mono<String> getItemById(@RequestParam("id") Long id,
                                    Model model) {
        return itemService.findByIdWithQuantity(orderService.findCartOrder(false), id)
                .map(itemDto -> {
                    model.addAttribute("item", itemDto);
                    return "item";
                });
    }
}