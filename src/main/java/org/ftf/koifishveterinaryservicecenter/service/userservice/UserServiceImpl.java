package org.ftf.koifishveterinaryservicecenter.service.userservice;

import org.ftf.koifishveterinaryservicecenter.dto.FeedbackDto;
import org.ftf.koifishveterinaryservicecenter.dto.UserDTO;
import org.ftf.koifishveterinaryservicecenter.entity.Address;
import org.ftf.koifishveterinaryservicecenter.entity.Feedback;
import org.ftf.koifishveterinaryservicecenter.entity.Role;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentMethod;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentStatus;
import org.ftf.koifishveterinaryservicecenter.exception.*;
import org.ftf.koifishveterinaryservicecenter.mapper.UserMapper;
import org.ftf.koifishveterinaryservicecenter.repository.*;
import org.ftf.koifishveterinaryservicecenter.service.fileservice.FileDownloadService;
import org.ftf.koifishveterinaryservicecenter.service.fileservice.FileUploadService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final UserMapper userMapper;
    private final AuthenticationService authenticationService;
    private final FishRepository fishRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final VeterinarianSlotsRepository veterinarianSlotsRepository;
    private final FeedbackRepository feedbackRepository;
    private final FileDownloadService fileDownloadService;

    public UserServiceImpl(UserRepository userRepository, AddressRepository addressRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, FileUploadService fileUploadService, UserMapper userMapper, AuthenticationService authenticationService, FishRepository fishRepository, AppointmentRepository appointmentRepository, PaymentRepository paymentRepository, VeterinarianSlotsRepository veterinarianSlotsRepository, FeedbackRepository feedbackRepository, FileDownloadService fileDownloadService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.userMapper = userMapper;
        this.authenticationService = authenticationService;
        this.fishRepository = fishRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.veterinarianSlotsRepository = veterinarianSlotsRepository;
        this.feedbackRepository = feedbackRepository;
        this.fileDownloadService = fileDownloadService;
    }


    @Override
    public User getUserProfile(Integer userId) {
        User user = userRepository.findUsersByUserId(userId);
//        if (user.getAvatar() != null && !user.getAvatar().startsWith("http://")) {
//            String avatarPath = fileDownloadService.getImageUrl(user.getAvatar());
//            user.setAvatar(avatarPath);
//        }
        return user;
    }

    @Override
    public UserDTO getMyInfo() {
        // Retrieve the authentication context
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        // Fetch the user by email
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Map the user entity to the UserDTO
        UserDTO userResponse = userMapper.convertEntityToDto(user);

        // Additional logic: set noPassword and noDob fields
        userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));

        return userResponse;
    }

    public void createPassword(String password) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        // Fetch the user by email
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if the user already has a password set
        if (StringUtils.hasText(user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD); // User already has a password
        }

        // Kiểm tra password
        if (password == null || password.isBlank()) {
            throw new AuthenticationException("Password can not be empty");
        }
        if (password.length() < 8) {
            throw new AuthenticationException("Password can not be less than 8 characters");
        }
        String passwordPattern = "^(?=.*[@#$%^&+=!{}]).{8,}$";
        if (!password.matches(passwordPattern)) {
            throw new AuthenticationException("Password must contain at least one special character and be at least 8 characters long");
        }

        // Encode the password and save the user
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }


    @Override
    public List<User> getAllVeterinarians() {
        Role role = roleRepository.findByRoleKey("VET");
        List<User> veterinarians = new ArrayList<>(role.getUsers());
//        veterinarians.forEach(veterinarian -> {
//            if (veterinarian.getAvatar() != null && !veterinarian.getAvatar().startsWith("http://")) {
//                veterinarian.setAvatar(fileDownloadService.getImageUrl(veterinarian.getAvatar()));
//            }
//        });
        return veterinarians;
    }

    @Override
    @Transactional
    public User updateAddress(Integer userId, Address convertedAddress) {
        User userFromDb = userRepository.findUsersByUserId(userId);

        if (userFromDb == null) {
            throw new UserNotFoundException("Not found user with Id: " + userId);
        }

        // set addressId for Address input
        Integer addressId = userFromDb.getCurrentAddress().getAddressId();
        convertedAddress.setAddressId(addressId);

        // update Address property for User instance
        Address updatedAddress = addressRepository.save(convertedAddress);
        userFromDb.setCurrentAddress(updatedAddress);
        return userFromDb;
    }

    @Override
    @Transactional
    public User updateUserProfile(Integer userId, User convertedCustomer) {
        User userFromDb = userRepository.findUsersByUserId(userId);

        if (userFromDb == null) {
            throw new UserNotFoundException("Not found user with Id: " + userId);
        }

        // set addressId for User input
        Integer customerId = userFromDb.getUserId();
        convertedCustomer.setUserId(userId);

        // fill in empty fields


        // check firstname
        if (convertedCustomer.getFirstName() != null) {
            userFromDb.setFirstName(convertedCustomer.getFirstName());
        }

        // check lastname
        if (convertedCustomer.getLastName() != null) {
            userFromDb.setLastName(convertedCustomer.getLastName());
        }

        // check phone number
        String phoneNumber = convertedCustomer.getPhoneNumber();
        if (!phoneNumber.equals(userFromDb.getPhoneNumber()) && !userRepository.existsUserByPhoneNumber(phoneNumber)) {
            userFromDb.setPhoneNumber(phoneNumber);
        }

        userFromDb = userRepository.save(userFromDb);

        // update user's profile for User instance
        return userFromDb;
    }

    @Override
    public List<User> getAllCustomers() {
        Role role = roleRepository.findByRoleKey("CUS");
        List<User> customers = new ArrayList<>(role.getUsers());
//        customers.forEach(customer -> {
//            if (customer.getAvatar() != null && !customer.getAvatar().startsWith("http://")) {
//                customer.setAvatar(fileDownloadService.getImageUrl(customer.getAvatar()));
//            }
//        });
        return customers;
    }

    @Override
    public void signUp(String username, String password, String email, String first_Name, String last_Name) {

        // Kiểm tra username
        if (username == null || username.isBlank()) {
            throw new AuthenticationException("Username can not be empty");
        }
        if (username.contains(" ")) {
            throw new AuthenticationException("Username can not contain white space");
        }
        if (userRepository.findUserByUsername(username) != null) {
            throw new AuthenticationException("Username is existed");
        }


        // Kiểm tra password
        if (password == null || password.isBlank()) {
            throw new AuthenticationException("Password can not be empty");
        }
        if (password.length() < 8) {
            throw new AuthenticationException("Password can not be less than 8 characters");
        }
        String passwordPattern = "^(?=.*[@#$%^&+=!{}]).{8,}$";
        if (!password.matches(passwordPattern)) {
            throw new AuthenticationException("Password must contain at least one special character and be at least 8 characters long");
        }

        // Kiểm tra email
        if (email == null || email.isBlank()) {
            throw new AuthenticationException("Email can not be empty");
        }
        String emailPattern = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if (!email.matches(emailPattern)) {
            throw new AuthenticationException("Email is not valid");
        }
//        if (userRepository.findUserByEmail(email) != null) {
//            throw new AuthenticationException("Email is already registered");
//        }
        userRepository.findUserByEmail(email).ifPresent(user -> {
            throw new AuthenticationException("Email is already registered");
        });

        // Kiểm tra first_name
        if (first_Name == null || first_Name.isBlank()) {
            throw new AuthenticationException("first_Name can not be empty");
        }

        // Kiểm tra last_name
        if (last_Name == null || last_Name.isBlank()) {
            throw new AuthenticationException("last_Name can not be empty");
        }

        // Tạo user mới và lưu vào database
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        Role role = roleRepository.findByRoleKey("CUS");
        user.setRole(role);
        user.setFirstName(first_Name);
        user.setLastName(last_Name);
        user.setEmail(email); // Gán email cho user
        userRepository.save(user);
    }


    @Override
    public User getVeterinarianById(Integer veterinarianId) {
        User veterinarian = userRepository.findVeterinarianById(veterinarianId);
        if (veterinarian == null) {
            throw new UserNotFoundException("Veterinarian not found with Id: " + veterinarianId);
        }
//        if (veterinarian.getAvatar() != null && !veterinarian.getAvatar().startsWith("http://")) {
//            String avatarPath = fileDownloadService.getImageUrl(veterinarian.getAvatar());
//            veterinarian.setAvatar(avatarPath);
//        }
        return veterinarian;
    }

    @Override
    public User getCustomerById(Integer customerId) {
        User customer = userRepository.findCustomerById(customerId);
        if (customer == null) {
            throw new UserNotFoundException("Customer not found with Id: " + customerId);
        }
//        if (customer.getAvatar() != null && !customer.getAvatar().startsWith("http://")) {
//            String avatarPath = fileDownloadService.getImageUrl(customer.getAvatar());
//            customer.setAvatar(avatarPath);
//        }
        return customer;
    }


    public UserDTO createStaff(String userName, String passWord, String firstName, String lastName) {
        User user = new User();

        user.setUsername(userName);
        user.setPassword(passWord);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(roleRepository.findByRoleKey("STA")); // Set role trực tiếp tại đây
        // encrypt password
        userRepository.save(user);

        // Sử dụng MapStruct để chuyển đổi từ User sang UserDTO
        return userMapper.convertEntityToDto(user); // Giả sử userMapper là một instance của MapStruct
    }

    @Override
    public User updateUserAvatar(Integer userId, MultipartFile image) throws IOException {
        User user = userRepository.findUsersByUserId(userId);
        if (user == null) {
            throw new UserNotFoundException("Not found user with Id: " + userId);
        }
        String path = fileUploadService.uploadFile(image);
        user.setAvatar(path);
        userRepository.save(user);
        return user;
    }

    @Override
    public List<Address> getAllAddresses(Integer customerId) {
        List<Address> addresses = addressRepository.findByCustomerId(customerId);
        if (addresses.isEmpty()) {
            throw new AddressNotFoundException("Address not found with customer ID: " + customerId);
        }
        return addresses;
    }

    @Override
    public Address getAddressById(Integer addressId) {
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address == null) {
            throw new AddressNotFoundException("Address not found with ID: " + addressId);
        }
        return address;
    }

    @Override
    public Address updateAddressDetails(Integer addressId, Address newAddress) {
        Address existedAddress = addressRepository.findById(addressId).orElse(null);
        if (existedAddress == null) {
            throw new AddressNotFoundException("Address not found with ID: " + addressId);
        } else {
            existedAddress.setCity(newAddress.getCity());
            existedAddress.setWard(newAddress.getWard());
            existedAddress.setDistrict(newAddress.getDistrict());
            existedAddress.setHomeNumber(newAddress.getHomeNumber());

            newAddress = addressRepository.save(existedAddress);

            return newAddress;
        }
    }

    @Override
    public Address setCurrentAddress(Integer customerId, Integer addressId) throws UserNotFoundException {
        User customer = this.getCustomerById(customerId);
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address == null) {
            throw new AddressNotFoundException("Address not found with ID: " + addressId);
        } else {
            customer.setCurrentAddress(address);
            userRepository.save(customer);
            return address;
        }
    }

    @Override
    public Address addAddress(Integer customerId, Address address) throws UserNotFoundException {
        User customer = this.getCustomerById(customerId);

        // Save address into database
        address.setEnabled(true);
        address.setCustomer(customer);
        address = addressRepository.save(address);

        // Set new address as main current address
        this.setCurrentAddress(customerId, address.getAddressId());

        return address;
    }

    @Override
    public Address disableAddress(Integer addressId) {
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address == null) {
            throw new AddressNotFoundException("Address not found with ID: " + addressId);
        }
        address.setEnabled(false);
        address = addressRepository.save(address);
        return address;
    }

    @Override
    public List<User> getAllStaffs() {
        // Lấy danh sách staffs dựa vào Role
        Role staffRole = roleRepository.findByRoleKey("STA");

        // Kiểm tra nếu role không tồn tại
        if (staffRole == null) {
            return new ArrayList<>(); // Trả về danh sách rỗng nếu không tìm thấy role
        }

        List<User> staffs = new ArrayList<>(staffRole.getUsers());

//        staffs.forEach(staff -> {
//            if (staff.getAvatar() != null && !staff.getAvatar().startsWith("http://")) {
//                staff.setAvatar(fileDownloadService.getImageUrl(staff.getAvatar()));
//            }
//        });

        // Trả về danh sách users từ role "STA"
        return staffs;
    }

    @Override
    public UserDTO updateUserInfo(int userId, boolean enabled) {
        // Lấy thông tin người dùng từ database
        User userFromDb = userRepository.findById(userId).orElse(null);


        if (userFromDb == null) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        Role role = userFromDb.getRole();
        if (role.getRoleId() == 1 || role.getRoleId() == 2) {
            throw new RoleException("This role don't need to change status");
        }

        // Cập nhật thông tin người dùng
        userFromDb.setEnabled(enabled);

        // Lưu người dùng đã cập nhật
        User updatedUser = userRepository.save(userFromDb);

        // Sử dụng mapper để chuyển đổi entity sang DTO
        return userMapper.convertEntityToDto(updatedUser);
    }


    @Override
    public UserDTO updatePassword(String newPassword) {
        // Lấy userId từ token
        int userId = authenticationService.getAuthenticatedUserId();

        User userFromDb = userRepository.findUsersByUserId(userId);

        if (userFromDb == null) {
            throw new UserNotFoundException("Không tìm thấy người dùng với Id: " + userId);
        }

        // Kiểm tra mật khẩu mới không được null hoặc trống
        if (newPassword == null || newPassword.isBlank()) {
            throw new AuthenticationException("Mật khẩu không được để trống");
        }

        // Kiểm tra độ dài mật khẩu mới
        if (newPassword.length() < 8) {
            throw new AuthenticationException("Mật khẩu không được ngắn hơn 8 ký tự");
        }

        // Kiểm tra mật khẩu có chứa ít nhất một ký tự đặc biệt
        String passwordPattern = "^(?=.*[@#$%^&+=!{}]).{8,}$";
        if (!newPassword.matches(passwordPattern)) {
            throw new AuthenticationException("Mật khẩu phải chứa ít nhất một ký tự đặc biệt và có độ dài tối thiểu là 8 ký tự");
        }

        // Mã hóa mật khẩu mới và cập nhật
        userFromDb.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userFromDb);

        // Chuyển đổi User sang UserDTO
        return UserMapper.INSTANCE.convertEntityToDto(userFromDb); // Giả sử bạn có một mapper cho User
    }

    @Override
    public List<User> getBookedVeterinarianBySlotId(Integer slotId) {
        List<User> veterinarians = userRepository.findBookedVeterinarian(slotId);

        if (veterinarians.isEmpty()) { // Empty
            throw new UserNotFoundException("There are no Booked veterinarian in slot with id: " + slotId);
        }

        return veterinarians;
    }

    @Override
    public Map<String, String> getUserAndFishStatistics() {
        Map<String, String> statistics = new HashMap<>();

        long totalFish = fishRepository.countEnabledFish();
        long totalStaff = userRepository.countEnabledStaff();
        long totalVets = userRepository.countEnabledVets();
        long totalCustomers = userRepository.countEnabledCustomers();

        statistics.put("totalFish", String.valueOf(totalFish));
        statistics.put("totalStaff", String.valueOf(totalStaff));
        statistics.put("totalVets", String.valueOf(totalVets));
        statistics.put("totalCustomers", String.valueOf(totalCustomers));

        return statistics;
    }

    @Override
    public Map<String, String> getAppointmentStatistics() {
        Map<String, String> appointmentStatistics = new HashMap<>();

        // Tính số lượng cuộc hẹn
        long totalAppointments = appointmentRepository.count();
        long totalAppointmentsToday = appointmentRepository.countAppointmentsToday();

        // Tính số lượng cuộc hẹn theo từng dịch vụ
        long service1Appointments = appointmentRepository.countByService_ServiceId(1);
        long service2Appointments = appointmentRepository.countByService_ServiceId(2);
        long service3Appointments = appointmentRepository.countByService_ServiceId(3);
        long taikhamAppointments = appointmentRepository.countByService_ServiceId(4);

        // Tính số lượng cuộc hẹn theo từng dịch vụ trong ngày hôm nay
        long service1AppointmentsToday = appointmentRepository.countByService_ServiceIdToday(1);
        long service2AppointmentsToday = appointmentRepository.countByService_ServiceIdToday(2);
        long service3AppointmentsToday = appointmentRepository.countByService_ServiceIdToday(3);
        long taikhamAppointmentsToday = appointmentRepository.countByService_ServiceIdToday(4);

        // Tính số lượng cuộc hẹn theo tháng
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        long appointmentsThisMonth = appointmentRepository.countByMonth(currentMonth, currentYear);

        // Tính số lượng cuộc hẹn theo quý
        int currentQuarter = (currentMonth - 1) / 3 + 1;
        long appointmentsThisQuarter = appointmentRepository.countByQuarter(currentQuarter, currentYear);

        // Tính số lượng cuộc hẹn cho từng dịch vụ trong tháng hiện tại
        long service1AppointmentsThisMonth = appointmentRepository.countByServiceAndMonth(1, currentMonth, currentYear);
        long service2AppointmentsThisMonth = appointmentRepository.countByServiceAndMonth(2, currentMonth, currentYear);
        long service3AppointmentsThisMonth = appointmentRepository.countByServiceAndMonth(3, currentMonth, currentYear);

        // Tìm dịch vụ được sử dụng nhiều nhất
        long maxAppointments = Math.max(service1AppointmentsThisMonth, Math.max(service2AppointmentsThisMonth, service3AppointmentsThisMonth));
        String mostUsedService;
        if (maxAppointments == service1AppointmentsThisMonth) {
            mostUsedService = "Service 1";
        } else if (maxAppointments == service2AppointmentsThisMonth) {
            mostUsedService = "Service 2";
        } else {
            mostUsedService = "Service 3";
        }

        // Thêm các giá trị vào map appointmentStatistics
        appointmentStatistics.put("totalAppointments", String.valueOf(totalAppointments));
        appointmentStatistics.put("totalAppointmentsToday", String.valueOf(totalAppointmentsToday));
        appointmentStatistics.put("service1Appointments", String.valueOf(service1Appointments));
        appointmentStatistics.put("service2Appointments", String.valueOf(service2Appointments));
        appointmentStatistics.put("service3Appointments", String.valueOf(service3Appointments));
        appointmentStatistics.put("taikhamAppointments", String.valueOf(taikhamAppointments));
        appointmentStatistics.put("service1AppointmentsToday", String.valueOf(service1AppointmentsToday));
        appointmentStatistics.put("service2AppointmentsToday", String.valueOf(service2AppointmentsToday));
        appointmentStatistics.put("service3AppointmentsToday", String.valueOf(service3AppointmentsToday));
        appointmentStatistics.put("taikhamAppointmentsToday", String.valueOf(taikhamAppointmentsToday));

        // Thêm số liệu theo tháng và quý
        appointmentStatistics.put("appointmentsThisMonth", String.valueOf(appointmentsThisMonth));
        appointmentStatistics.put("appointmentsThisQuarter", String.valueOf(appointmentsThisQuarter));

        // Thêm thông tin dịch vụ được sử dụng nhiều nhất
        appointmentStatistics.put("mostUsedService", mostUsedService);

        return appointmentStatistics;
    }


    @Override
    public Map<String, String> getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, String> paymentStatistics = new HashMap<>();

        // Tính số lượng thanh toán trong khoảng thời gian
        long totalPayments = paymentRepository.countPaymentsInRange(startDate, endDate);

        // Tính tổng số tiền thanh toán trong khoảng thời gian
        double totalAmount = (paymentRepository.sumTotalAmountInRange(startDate, endDate) != null)
                ? paymentRepository.sumTotalAmountInRange(startDate, endDate)
                : 0.0;

        // Tính số lượng thanh toán theo phương thức trong khoảng thời gian
        long cashPayments = paymentRepository.countByPaymentMethodInRange(PaymentMethod.CASH, startDate, endDate);
        long vnPayPayments = paymentRepository.countByPaymentMethodInRange(PaymentMethod.VN_PAY, startDate, endDate);

        // Thống kê theo trạng thái "PAID" và "NOT_PAID" trong khoảng thời gian
        long paidPayments = paymentRepository.countByStatusInRange(PaymentStatus.PAID, startDate, endDate);
        long notPaidPayments = paymentRepository.countByStatusInRange(PaymentStatus.NOT_PAID, startDate, endDate);

        // Thêm các giá trị vào map paymentStatistics
        paymentStatistics.put("totalPayments", String.valueOf(totalPayments));
        paymentStatistics.put("totalAmount", String.valueOf(totalAmount));

        // Thêm thống kê theo phương thức thanh toán
        paymentStatistics.put("cashPayments", String.valueOf(cashPayments));
        paymentStatistics.put("vnPayPayments", String.valueOf(vnPayPayments));

        // Thống kê theo trạng thái
        paymentStatistics.put("paidPayments", String.valueOf(paidPayments));
        paymentStatistics.put("notPaidPayments", String.valueOf(notPaidPayments));

        return paymentStatistics;
    }
    @Override
    public long getVetSlotsInRange(int vetId, LocalDate startDate, LocalDate endDate) {
        long totalSlots = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            totalSlots += veterinarianSlotsRepository.countSlotsByVetInDate(
                    vetId,
                    date.getYear(),
                    date.getMonthValue(),
                    date.getDayOfMonth()
            );
        }

        return totalSlots;
    }

    //them so luong feedback voi so luong sao trung binh cua bac si
    @Override
    public Map<String, Object> getFeedbackStatistics() {
        Map<String, Object> feedbackStatistics = new HashMap<>();

        // Tính số lượng feedback
        long totalFeedbackToday = feedbackRepository.countFeedbackToday();

        // Tính số lượng feedback theo tháng
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        long totalFeedbackThisMonth = feedbackRepository.countFeedbackByMonth(currentMonth, currentYear);

        // Tính số lượng feedback theo quý
        int currentQuarter = (currentMonth - 1) / 3 + 1;
        long totalFeedbackThisQuarter = feedbackRepository.countFeedbackByQuarter(currentQuarter, currentYear);


        // Thêm các giá trị vào map feedbackStatistics
        feedbackStatistics.put("totalFeedbackToday", totalFeedbackToday);
        feedbackStatistics.put("totalFeedbackThisMonth", totalFeedbackThisMonth);
        feedbackStatistics.put("totalFeedbackThisQuarter", totalFeedbackThisQuarter);

        return feedbackStatistics;
    }

    @Override
    public Optional<String> getLinkMeetByVetId(Integer vetId) {
//        return vetMeetingRepository.findByVetId(vetId)
//                .map(Meeting::getLinkMeet);
        User veterinarian = this.getVeterinarianById(vetId);
        return Optional.of(veterinarian.getMeeting().getLinkMeet());
    }

    @Override
    public BigDecimal getAverageRatingForService(Integer serviceId) {
        return feedbackRepository.findAverageRatingByServiceId(serviceId);
    }

    @Override
    public List<FeedbackDto> getFeedbacksAboveRatingForService(Integer serviceId) {
        List<Feedback> feedbacks = feedbackRepository.findFeedbacksAboveRatingByServiceId(serviceId);
        return feedbacks.stream()
                .map(feedback -> {
                    FeedbackDto dto = new FeedbackDto();
                    dto.setFeedbackId(feedback.getFeedbackId());
                    dto.setRating(feedback.getRating());
                    dto.setComment(feedback.getComment());
                    dto.setDatetime(feedback.getDatetime());
                    // Có thể thêm các thuộc tính khác nếu cần
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Double getAverageRatingForVeterinarian(Integer veterinarianId) {
        return feedbackRepository.findAverageRatingByVeterinarianId(veterinarianId);
    }

}