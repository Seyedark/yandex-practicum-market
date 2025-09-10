package ru.yandex.practicum.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.service.OrderService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final OrderService orderService;
    private final CoordinatorService coordinatorService;

    @GetMapping
    @RequestMapping("/")
    public String getAllItemsByConditions(@RequestParam(name = "search", required = false) String search,
                                          @RequestParam(name = "sortBy", defaultValue = "NO") String sort,
                                          @RequestParam(name = "page", defaultValue = "1") int page,
                                          @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                          Model model) {
        OrderEntity orderEntity = orderService.findCartOrder();

        Page<ItemEntity> itemEntityList = itemService.getAllItemsByConditions(orderEntity, search, sort, page, pageSize);
        int totalPages = itemEntityList.getTotalPages() == 0 ? 1 : itemEntityList.getTotalPages();

        model.addAttribute("currentSize", pageSize);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("sort", sort);
        model.addAttribute("items", itemEntityList);
        return "main";
    }

    @PostMapping
    @RequestMapping("/item")
    public String getItemById(@RequestParam("id") Long id,
                                          Model model) {
        ItemEntity itemEntity = coordinatorService.getItemById(id);
        model.addAttribute("item", itemEntity);
        return "item";
    }
}