package com.huydung.custmapapplication.user.service.impl;

import com.huydung.custmapapplication.user.converter.UserConverter;
import com.huydung.custmapapplication.user.dto.ChangePasswordRequest;
import com.huydung.custmapapplication.user.dto.CreateUserRequest;
import com.huydung.custmapapplication.user.dto.ResetPasswordRequest;
import com.huydung.custmapapplication.user.dto.UserDTO;
import com.huydung.custmapapplication.user.entity.RoleEntity;
import com.huydung.custmapapplication.user.entity.UserEntity;
import com.huydung.custmapapplication.user.repository.RoleRepository;
import com.huydung.custmapapplication.user.repository.UserRepository;
import com.huydung.custmapapplication.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    // Mật khẩu mặc định cũ từng được gán cứng trong code: tài khoản nào còn dùng sẽ bị buộc đổi
    private static final String LEGACY_DEFAULT_PASSWORD = "123456";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final String initialPassword;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           UserConverter userConverter,
                           @Value("${app.security.initial-password:}") String initialPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userConverter = userConverter;
        this.initialPassword = initialPassword == null ? "" : initialPassword;
    }

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại: " + request.getUsername());
        }
        validateNewPassword(request.getPassword(), request.getUsername());

        UserEntity user = userConverter.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Mật khẩu do admin đặt nên người dùng phải đổi ở lần đăng nhập đầu tiên
        user.setMustChangePassword(true);

        Set<RoleEntity> roles = new HashSet<>();
        if (request.getRoleCodes() == null || request.getRoleCodes().isEmpty()) {
            RoleEntity staffRole = roleRepository.findByCode("ROLE_STAFF")
                    .orElseThrow(() -> new RuntimeException("Chưa khởi tạo ROLE_STAFF"));
            roles.add(staffRole);
        } else {
            for (String code : request.getRoleCodes()) {
                roleRepository.findByCode(code).ifPresent(roles::add);
            }
        }
        user.setRoles(roles);

        return userConverter.toDTO(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(String keyword, Integer status) {
        return userRepository.searchUsersCustom(keyword, status)
                .stream()
                .map(userConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu đổi mật khẩu");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        String current = request.getCurrentPassword();
        if (current == null || !passwordEncoder.matches(current, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || !newPassword.equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        if (newPassword.equals(current)) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        validateNewPassword(newPassword, user.getUsername());

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, ResetPasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu đặt lại mật khẩu");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + userId));

        validateNewPassword(request.getNewPassword(), user.getUsername());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void initDefaultUsersAndRoles() {
        // 1. Tạo 2 Role mặc định
        RoleEntity adminRole = roleRepository.findByCode("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_ADMIN", "Quản trị viên (Full quyền)")));

        RoleEntity staffRole = roleRepository.findByCode("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_STAFF", "Nhân viên phụ trách địa bàn")));

        // 2. Chỉ tạo tài khoản mặc định khi đã cấu hình mật khẩu khởi tạo qua biến môi trường
        if (initialPassword.isBlank()) {
            if (!userRepository.existsByUsername("admin")) {
                log.warn("Chưa đặt biến môi trường APP_INITIAL_PASSWORD nên KHÔNG tạo tài khoản mặc định. "
                        + "Hệ thống hiện chưa có tài khoản admin để đăng nhập.");
            }
        } else {
            createDefaultUser("admin", "Quản Trị Hệ Thống", Set.of(adminRole, staffRole));

            // Danh sách nhân viên khớp với cột "Giao nhân viên" trong file Excel
            String[][] defaultStaffs = {
                    {"dunglh", "Lâm Huy Dũng"},
                    {"vietbt", "Bá Thanh Việt"},
                    {"tungbt", "Bùi Thanh Tùng"},
                    {"trungnq", "Nguyễn Quang Trung"},
                    {"hungnt", "Nguyễn Trọng Hùng"},
                    {"longph", "Phạm Hoàng Long"},
                    {"ducth", "Trần Hữu Đức"}
            };
            for (String[] staff : defaultStaffs) {
                // Tài khoản dunglh vừa có quyền ADMIN (để import file) vừa có tên Lâm Huy Dũng để test filter
                Set<RoleEntity> roles = "dunglh".equals(staff[0]) ? Set.of(adminRole, staffRole) : Set.of(staffRole);
                createDefaultUser(staff[0], staff[1], roles);
            }
        }

        // 3. Tài khoản cũ còn dùng mật khẩu mặc định (123456 hoặc mật khẩu khởi tạo) -> buộc đổi mật khẩu
        forceChangeForDefaultPasswords();
    }

    private void createDefaultUser(String username, String fullName, Set<RoleEntity> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(initialPassword));
        u.setFullName(fullName);
        u.setStatus(1);
        u.setMustChangePassword(true);
        u.setRoles(roles);
        userRepository.save(u);
    }

    private void forceChangeForDefaultPasswords() {
        for (UserEntity user : userRepository.findAll()) {
            if (Boolean.TRUE.equals(user.getMustChangePassword())) {
                continue;
            }
            boolean usesLegacy = passwordEncoder.matches(LEGACY_DEFAULT_PASSWORD, user.getPassword());
            boolean usesInitial = !initialPassword.isBlank() && passwordEncoder.matches(initialPassword, user.getPassword());
            if (usesLegacy || usesInitial) {
                user.setMustChangePassword(true);
                userRepository.save(user);
                log.info("Tài khoản '{}' đang dùng mật khẩu mặc định -> buộc đổi mật khẩu ở lần đăng nhập tới", user.getUsername());
            }
        }
    }

    // Quy tắc mật khẩu: >= 8 ký tự, có cả chữ và số, không trùng username, không trùng mật khẩu khởi tạo
    private void validateNewPassword(String password, String username) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu phải gồm cả chữ và số");
        }
        if (username != null && password.equalsIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("Mật khẩu không được trùng với tên đăng nhập");
        }
        if (!initialPassword.isBlank() && password.equals(initialPassword)) {
            throw new IllegalArgumentException("Không được dùng lại mật khẩu khởi tạo của hệ thống");
        }
    }
}
