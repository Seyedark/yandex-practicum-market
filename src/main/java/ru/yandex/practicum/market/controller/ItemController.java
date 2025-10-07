package ru.yandex.practicum.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.ItemDto;
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
                                                @RequestParam(name = "page", defaultValue = "1") int page,
                                                @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                                Model model) {
        Mono<Page<ItemDto>> itemDtoList = itemService.getAllItemsByConditions(orderService.findCartOrder(), search, sort, page, pageSize);
        return itemDtoList.flatMap(pageData -> {
            model.addAttribute("items", pageData.getContent());
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
        return itemService.findByIdWithQuantity(orderService.findCartOrder(), id)
                .map(itemDto -> {
                    model.addAttribute("item", itemDto);
                    return "item";
                });
    }
}