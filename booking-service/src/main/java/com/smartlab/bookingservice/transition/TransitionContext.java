package com.smartlab.bookingservice.transition;

import com.smartlab.bookingservice.dto.ItemDto;
import com.smartlab.bookingservice.dto.LabDto;
import com.smartlab.bookingservice.dto.UserDto;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.security.UserContext;

/**
 * Snapshot of everything a Transition.notifications(...) might need, prebuilt by
 * the engine so each transition reads pre-loaded values instead of issuing its
 * own Feign hits. Any of the User/Item/Lab fields can be null if the upstream
 * lookup failed — the transition decides how to degrade.
 */
public record TransitionContext(
        Booking booking,
        BookingItem line,
        String fromState,
        UserContext actor,
        UserDto student,
        UserDto instructor,
        UserDto hod,
        ItemDto item,
        LabDto lab
) {
    public String studentName()  { return student != null   ? student.getFullName()   : "the student"; }
    public String itemName()     { return item != null      ? item.getName()          : "the requested item"; }
    public String labName()      { return lab != null       ? lab.getName()           : "the requested lab"; }
    public String hodName()        { return hod != null        ? hod.getFullName()        : "the HoD"; }
    public String instructorName() { return instructor != null ? instructor.getFullName() : "your instructor"; }
}
