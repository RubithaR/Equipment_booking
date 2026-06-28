package com.smartlab.equipmentservice.service;

import com.smartlab.equipmentservice.dto.ItemRequest;
import com.smartlab.equipmentservice.dto.ItemResponse;
import com.smartlab.equipmentservice.entity.Item;
import com.smartlab.security.ItemStatus;
import com.smartlab.security.Roles;
import com.smartlab.equipmentservice.entity.Lab;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.equipmentservice.repository.ItemRepository;
import com.smartlab.equipmentservice.repository.LabRepository;
import com.smartlab.security.CurrentUser;
import com.smartlab.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {


    private final ItemRepository itemRepository;
    private final LabRepository labRepository;

    public ItemResponse create(ItemRequest request) {
        UserContext me = CurrentUser.require();
        Lab lab = getLabOrThrow(request.getLabId());
        ensureCanManageLab(me, lab);

        if (!ItemStatus.isValid(request.getStatus())) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }

        Item item = new Item();
        item.setLabId(lab.getId());
        item.setModel(request.getModel());
        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setSerialNumber(request.getSerialNumber());
        item.setStatus(request.getStatus());
        item.setDescription(request.getDescription());
        item.setConditionNote(request.getConditionNote());
        return ItemResponse.from(itemRepository.save(item));
    }

    public List<ItemResponse> list(Long labId, String status, String category, String model) {
        List<Item> rows;
        if (labId != null)         rows = itemRepository.findByLabId(labId);
        else if (status != null)   rows = itemRepository.findByStatus(status);
        else if (category != null) rows = itemRepository.findByCategory(category);
        else if (model != null)    rows = itemRepository.findByModel(model);
        else                       rows = itemRepository.findAll();
        return rows.stream().map(ItemResponse::from).collect(Collectors.toList());
    }

    public ItemResponse getById(Long id) {
        return ItemResponse.from(getOrThrow(id));
    }

    public ItemResponse update(Long id, ItemRequest request) {
        UserContext me = CurrentUser.require();
        Item item = getOrThrow(id);
        Lab lab = getLabOrThrow(item.getLabId());
        ensureCanManageLab(me, lab);

        if (request.getLabId() != null && !request.getLabId().equals(item.getLabId())) {
            throw new BadRequestException("Items cannot be moved between labs");
        }
        if (!ItemStatus.isValid(request.getStatus())) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }

        item.setModel(request.getModel());
        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setSerialNumber(request.getSerialNumber());
        item.setStatus(request.getStatus());
        item.setDescription(request.getDescription());
        item.setConditionNote(request.getConditionNote());
        return ItemResponse.from(itemRepository.save(item));
    }

    /**
     * Status update path used by booking-service Feign — kept open in SecurityConfig
     * so this can run without a JWT. Skip the role check when there is no principal.
     */
    public ItemResponse updateStatus(Long id, String status) {
        if (!ItemStatus.isValid(status)) {
            throw new BadRequestException("Invalid status: " + status);
        }
        Item item = getOrThrow(id);

        UserContext me = CurrentUser.get();
        if (me != null) {
            Lab lab = getLabOrThrow(item.getLabId());
            ensureCanManageLab(me, lab);
        }

        item.setStatus(status);
        return ItemResponse.from(itemRepository.save(item));
    }

    public void delete(Long id) {
        UserContext me = CurrentUser.require();
        Item item = getOrThrow(id);
        Lab lab = getLabOrThrow(item.getLabId());
        ensureCanManageLab(me, lab);
        itemRepository.delete(item);
    }

    // ===== helpers =====

    private Item getOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
    }

    private Lab getLabOrThrow(Long id) {
        return labRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lab not found: " + id));
    }

    private void ensureCanManageLab(UserContext me, Lab lab) {
        if (me.hasRole(Roles.MAIN_ADMIN)) return;
        if (me.hasRole(Roles.DEPT_ADMIN)
                && lab.getDepartmentId() != null
                && lab.getDepartmentId().equals(me.departmentId())) return;
        if (me.hasRole(Roles.INSTRUCTOR)
                && lab.getInstructorUserId() != null
                && lab.getInstructorUserId().equals(me.userId())) return;
        throw new BadRequestException("You are not allowed to manage items in this lab");
    }
}
