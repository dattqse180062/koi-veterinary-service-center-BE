package org.ftf.koifishveterinaryservicecenter.service.userservice;

import org.ftf.koifishveterinaryservicecenter.dto.UserDTO;
import org.ftf.koifishveterinaryservicecenter.entity.Address;
import org.ftf.koifishveterinaryservicecenter.entity.Role;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.exception.AddressNotFoundException;
import org.ftf.koifishveterinaryservicecenter.exception.AuthenticationException;
import org.ftf.koifishveterinaryservicecenter.exception.RoleException;
import org.ftf.koifishveterinaryservicecenter.exception.UserNotFoundException;
import org.ftf.koifishveterinaryservicecenter.mapper.UserMapper;
import org.ftf.koifishveterinaryservicecenter.repository.AddressRepository;
import org.ftf.koifishveterinaryservicecenter.repository.RoleRepository;
import org.ftf.koifishveterinaryservicecenter.repository.UserRepository;
import org.ftf.koifishveterinaryservicecenter.service.fileservice.FileDownloadService;
import org.ftf.koifishveterinaryservicecenter.service.fileservice.FileUploadService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final UserMapper userMapper;
    private final AuthenticationService authenticationService;
    private final FileDownloadService fileDownloadService;

    public UserServiceImpl(
            UserRepository userRepository
            , AddressRepository addressRepository
            , RoleRepository roleRepository
            , PasswordEncoder passwordEncoder
            , FileUploadService fileUploadService
            , UserMapper userMapper
            , AuthenticationService authenticationService
            , FileDownloadService fileDownloadService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.userMapper = userMapper;
        this.authenticationService = authenticationService;
        this.fileDownloadService = fileDownloadService;
    }


    @Override
    public User getUserProfile(Integer userId) {
        User user = userRepository.findUsersByUserId(userId);
        if (user.getAvatar() != null) {
            String avatarPath = fileDownloadService.getImageUrl(user.getAvatar());
            user.setAvatar(avatarPath);
        }
        return user;
    }

    @Override
    public List<User> getAllVeterinarians() {
        Role role = roleRepository.findByRoleKey("VET");
        List<User> veterinarians = new ArrayList<>(role.getUsers());
        veterinarians.forEach(veterinarian -> {
            if (veterinarian.getAvatar() != null) {
                veterinarian.setAvatar(fileDownloadService.getImageUrl(veterinarian.getAvatar()));
            }
        });
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
        customers.forEach(customer -> {
            if (customer.getAvatar() != null) {
                customer.setAvatar(fileDownloadService.getImageUrl(customer.getAvatar()));
            }
        });
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
        if (userRepository.findUserByEmail(email) != null) {
            throw new AuthenticationException("Email is already registered");
        }

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
        return veterinarian;
    }

    @Override
    public User getCustomerById(Integer customerId) {
        User customer = userRepository.findCustomerById(customerId);
        if (customer == null) {
            throw new UserNotFoundException("Customer not found with Id: " + customerId);
        }
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

        staffs.forEach(staff -> {
            if (staff.getAvatar() != null) {
                staff.setAvatar(fileDownloadService.getImageUrl(staff.getAvatar()));
            }
        });

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
}









