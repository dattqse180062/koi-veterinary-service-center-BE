package org.ftf.koifishveterinaryservicecenter.controller;


import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.ftf.koifishveterinaryservicecenter.dto.*;
import org.ftf.koifishveterinaryservicecenter.dto.response.AuthenticationResponse;
import org.ftf.koifishveterinaryservicecenter.dto.response.IntrospectResponse;
import org.ftf.koifishveterinaryservicecenter.entity.Address;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlots;
import org.ftf.koifishveterinaryservicecenter.exception.AddressNotFoundException;
import org.ftf.koifishveterinaryservicecenter.exception.AuthenticationException;
import org.ftf.koifishveterinaryservicecenter.exception.TimeSlotNotFoundException;
import org.ftf.koifishveterinaryservicecenter.exception.UserNotFoundException;
import org.ftf.koifishveterinaryservicecenter.mapper.AddressMapper;
import org.ftf.koifishveterinaryservicecenter.mapper.UserMapper;
import org.ftf.koifishveterinaryservicecenter.service.appointmentservice.AppointmentService;
import org.ftf.koifishveterinaryservicecenter.service.feedbackservice.FeedbackService;
import org.ftf.koifishveterinaryservicecenter.service.slotservice.SlotService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.AuthenticationServiceImpl;
import org.ftf.koifishveterinaryservicecenter.service.userservice.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationServiceImpl authenticationService;
    private final FeedbackService feedbackService;
    private final AppointmentService appointmentService;
    private final SlotService slotService;


    public UserController(UserService userService, UserMapper userMapper, AuthenticationServiceImpl authenticationService, FeedbackService feedbackService, AppointmentService appointmentService, SlotService slotService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.authenticationService = authenticationService;
        this.feedbackService = feedbackService;
        this.appointmentService = appointmentService;
        this.slotService = slotService;
    }


    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam Integer userId) {

        Integer userIdFromToken = userId;  // the userId takes from Authentication object in SecurityContext
        User user = userService.getUserProfile(userId);
        UserDTO userDto = UserMapper.INSTANCE.convertEntityToDto(user);
        return ResponseEntity.ok(userDto);
    }

// Already have API in AddressController
//    @PutMapping("/address")
//    public ResponseEntity<?> updateAddressForCustomer(@RequestParam Integer userId, @RequestBody AddressDTO addressFromRequest) {
//
//        Address convertedAddress = AddressMapper.INSTANCE.convertDtoToEntity(addressFromRequest);
//
//        Integer userIdFromToken = 1; // the userId takes from Authentication object in SecurityContext
//
//        // check(userIdFromToken, userId)
//
//        User updatedCustomer = userService.updateAddress(userId, convertedAddress);
//        UserDTO userDto = UserMapper.INSTANCE.convertEntityToDto(updatedCustomer);
//        return ResponseEntity.ok(userDto);
//    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestParam Integer userId, @RequestBody UserDTO userFromRequest) {


        try {
            User convertedCustomer = UserMapper.INSTANCE.convertDtoToEntity(userFromRequest);
            Integer userIdFromToken = 1; // the userId takes from Authentication object in SecurityContext

//            if (!userId.equals(userIdFromToken)) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//            }

            // check(userIdFromToken, userId)
            User updatedCustomer = userService.updateUserProfile(userId, convertedCustomer);
            UserDTO userDto = UserMapper.INSTANCE.convertEntityToDto(updatedCustomer);
            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

    }


    @GetMapping("/customers")
    public ResponseEntity<?> getAllCustomers() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Log thông tin về username và role
        log.info("Userid: {}", authentication.getName()); // Đúng cú pháp cho log.info
        authentication.getAuthorities().forEach(grantedAuthority -> log.info("role: {}", grantedAuthority.getAuthority()));
        List<User> customers = userService.getAllCustomers();
        if (customers.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            List<UserDTO> userDTOs = customers.stream().map(userMapper::convertEntityToDto).collect(Collectors.toList());
            return new ResponseEntity<>(userDTOs, HttpStatus.OK);
        }
    }

    @PostMapping("/token")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequestDTO request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }


    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequestDTO request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        if (result == null) {
            return ApiResponse.<IntrospectResponse>builder().code(404).build();
        }
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody UserDTO userDTOFromRequest) {
        try {
            String username = userDTOFromRequest.getUsername();
            String password = userDTOFromRequest.getPassword();
            String email = userDTOFromRequest.getEmail();
            String firstName = userDTOFromRequest.getFirstName();
            String lastName = userDTOFromRequest.getLastName();

            userService.signUp(username, password, email, firstName, lastName);
            return new ResponseEntity<>("Sign up successfully", HttpStatus.OK);
        } catch (AuthenticationException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /*
     * Update avatar of user
     * Actors: Customer, Manager
     * */
    //@PreAuthorize("hasAuthority('CUS')")
    @PutMapping("/avatar")
    public ResponseEntity<?> updateUserAvatar(@RequestParam("user_id") Integer userId, @RequestParam("image") MultipartFile image) {
        try {
            User user = userService.updateUserAvatar(userId, image);
            UserDTO userDto = UserMapper.INSTANCE.convertEntityToDtoIgnoreAddress(user);
            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
     * Actors: Manager
     * */
    @GetMapping("/veterinarians")
    public ResponseEntity<?> getAllVeterinarians() {
        try {
            List<User> veterinarians = userService.getAllVeterinarians();
            List<UserDTO> userDTOs = veterinarians.stream().map(UserMapper.INSTANCE::convertEntityToDtoIgnoreAddress).collect(Collectors.toList());
            return new ResponseEntity<>(userDTOs, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // get all available VET based on slotId
    // for Staff
    @GetMapping("/veterinarian/{slotId}")
    public ResponseEntity<?> getAllAvailableVeterinariansBasedOnSlotId(@PathVariable Integer slotId) {
        try {
            List<VeterinarianSlots> veterinarianSlots = slotService.getVeterinarianSlotsBySlotId(slotId);

            // from vetId -> list veterinarian
            List<User> veterinarians = veterinarianSlots.stream().map(vetSlot -> userService.getVeterinarianById(vetSlot.getVeterinarian().getUserId())).toList();

            List<UserDTO> vetDtos = veterinarians.stream().map(UserMapper.INSTANCE::convertToVeterinarianDto).toList();
            return new ResponseEntity<>(vetDtos, HttpStatus.OK);
        } catch (TimeSlotNotFoundException exception) {
            return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


    /*
     * Update an address information
     * Actors: Customer
     * */
    @PutMapping("/address")
    public ResponseEntity<?> updateAddress(@RequestParam Integer addressId) {
        try {
            Integer customerId = authenticationService.getAuthenticatedUserId();
            Address address = userService.getAddressById(addressId);
            if (address.getCustomer().getUserId().equals(customerId)) {
                address = userService.setCurrentAddress(customerId, addressId);
                AddressDTO addressDto = AddressMapper.INSTANCE.convertEntityToDto(address);
                return new ResponseEntity<>(addressDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (AddressNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // view list staffs
    @GetMapping("/staffs")
    public ResponseEntity<?> getAllStaffsAndVeterinarians() {
        List<User> staffs = userService.getAllStaffs();

        if (staffs.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            List<UserDTO> userDTOS = staffs.stream()
                    .map(UserMapper.INSTANCE::convertEntityToDto)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(userDTOS, HttpStatus.OK);
        }
    }


    @PostMapping("/staff")
    public ResponseEntity<String> createStaff(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.createStaff(userDTO.getUsername(), userDTO.getPassword(), userDTO.getFirstName(), userDTO.getLastName());
        if (createdUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thêm mới nhân viên thất bại.");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body("Thêm mới nhân viên thành công.");
        }
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> logout(@RequestBody RefreshRequest request) throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/deleteuser")
    public ResponseEntity<String> updateUser(@RequestBody UserDTO userDTO) {
        try {
            // Gọi phương thức updateUser từ service với dữ liệu từ UserDTO
            userService.updateUserInfo(userDTO.getUserId(), userDTO.isEnable());

            // Trả về thông báo thành công
            return ResponseEntity.ok("User updated successfully.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Error updating user.");
        }
    }


    //update password
    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(@RequestBody UserDTO userDTO) {
        try {
            userService.updatePassword(userDTO.getPassword()); // Chỉ cần gọi dịch vụ mà không cần lưu trữ kết quả
            return ResponseEntity.ok("Password updated successfully."); // Trả về thông báo thành công
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND); // Trả về thông báo lỗi nếu không tìm thấy người dùng
        } catch (AuthenticationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // Trả về thông báo lỗi cho các vấn đề xác thực
        } catch (Exception e) {
            e.printStackTrace(); // Ghi log chi tiết thông tin ngoại lệ
            return new ResponseEntity<>("An error occurred while updating the password.", HttpStatus.INTERNAL_SERVER_ERROR); // Thông báo lỗi chung
        }


    }

    @PostMapping("/outbound/authentication")
    ApiResponse<AuthenticationResponse> outboundAuthenticate(
            @RequestParam("code") String code) {
        var result = authenticationService.outboundAuthenticate(code);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();

    }

    @PostMapping("/create-password")
    public ResponseEntity<ApiResponse<Void>> createPassword(@RequestBody @Valid String password) {
        userService.createPassword(password);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Password has been created!")
                        .build()
        );
    }


    @GetMapping("/my-info")
    public ResponseEntity<UserDTO> getMyInfo() {
        try {
            // Call the service method which now returns UserDTO
            UserDTO userDto = userService.getMyInfo();
            return ResponseEntity.ok(userDto);  // Use ResponseEntity.ok() for successful responses
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", e.getMessage());  // Log the error message
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);  // Use status and body directly
        } catch (Exception e) {
            log.error("An error occurred while fetching user info: {}", e.getMessage());  // Log the general error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    //  lấy số liệu liên quan đến người dùng và cá
    @GetMapping("/user-fish-statistics")
    public Map<String, String> getUserAndFishStatistics() {
        return userService.getUserAndFishStatistics();
    }

    // Lấy số liệu liên quan đến cuộc hẹn
    @GetMapping("/appointment-statistics")
    public Map<String, String> getAppointmentStatistics() {
        return userService.getAppointmentStatistics();
    }

    // Lấy số liệu liên quan đến thanh toán
    @GetMapping("/paymentstatistics")
    public ResponseEntity<Map<String, String>> getPaymentStatistics(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {

        // Kiểm tra nếu startDate lớn hơn endDate thì trả về lỗi
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body(Map.of("error", "startDate không được lớn hơn endDate"));
        }

        // Gọi service để lấy thống kê
        Map<String, String> statistics = userService.getPaymentStatistics(startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    // Kiểm tra vet được đặt bao nhiêu lần trong khoảng thời gian cụ thể
    @GetMapping("/{vetId}/slots")
    public ResponseEntity<Long> getVetSlotsInRange(
            @PathVariable int vetId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body(0L);
        }

        long slotsCount = userService.getVetSlotsInRange(vetId, startDate, endDate);
        return ResponseEntity.ok(slotsCount);
    }

    @GetMapping("/feedback-statistics")
    public Map<String, Object> getFeedbackStatistics() {
        return userService.getFeedbackStatistics();
    }

    @GetMapping("/{vetId}/link")
    public ResponseEntity<String> getLinkMeetByVetId(@PathVariable Integer vetId) {
        return userService.getLinkMeetByVetId(vetId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //lay ra trung binh rating cua tung service
    @GetMapping("/average-rating/{serviceId}")
    public BigDecimal getAverageRatingService(@PathVariable Integer serviceId) {
        return userService.getAverageRatingForService(serviceId);
    }

    //Lay ra nhung feedback tot cua tung service
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<FeedbackDto>> getFeedbacksByService(@PathVariable Integer serviceId) {
        List<FeedbackDto> feedbacks = userService.getFeedbacksAboveRatingForService(serviceId);
        return ResponseEntity.ok(feedbacks);
    }

    //trung binh rating tung bac si
    @GetMapping("/{veterinarianId}/average-rating")
    public Double getAverageRating(@PathVariable Integer veterinarianId) {
        return userService.getAverageRatingForVeterinarian(veterinarianId);
    }
}
