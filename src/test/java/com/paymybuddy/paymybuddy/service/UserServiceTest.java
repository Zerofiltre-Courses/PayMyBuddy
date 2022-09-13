package com.paymybuddy.paymybuddy.service;

import com.paymybuddy.paymybuddy.exceptions.BuddyNotFoundException;
import com.paymybuddy.paymybuddy.exceptions.EmailAlreadyUsedException;
import com.paymybuddy.paymybuddy.model.User;
import com.paymybuddy.paymybuddy.model.viewmodel.UserViewModel;
import com.paymybuddy.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import(UserService.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {
    /**
     * Class under test.
     */
    @Autowired
    UserService userService;

    @MockBean
    UserRepository        userRepository;
    @MockBean
    BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;

    @BeforeEach
    public void initUsers() {
        testUser = new User();
        testUser.setFirstName("Chandler");
        testUser.setLastName("Bing");
        testUser.setPassword("CouldIBeAnyMoreBored");
        testUser.setEmail("bingchandler@friends.com");
        testUser.setBalance(new BigDecimal("2509.56"));

        otherUser = new User();
        otherUser.setFirstName("Joey");
        otherUser.setLastName("Tribbiani");
        otherUser.setPassword("HowUDoin");
        otherUser.setEmail("otheremail@mail.com");

    }

    @Test
    @DisplayName("Saving user with valid email should create new user")
    public void createUser_usingValidEmail_shouldCreate_newUser() {
        String emailAddress = "username@domain.com";
        String password     = "ABCDEF123";
        testUser.setEmail(emailAddress);
        doReturn(Optional.empty())
                .when(userRepository).findByEmail(emailAddress);
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn(password);
        doReturn(testUser)
                .when(userRepository).save(any(User.class));

        testUser = userService.createUser(emailAddress, password);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Saving user with invalid email should throw exception")
    public void createUser_usingValidEmail_shouldThrow_exception() {
        String emailAddress = "username@domain";
        testUser.setEmail(emailAddress);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser("username@domain", "123"));
    }

    @Test
    @DisplayName("Saving a user with unique email should create new user")
    void createUser_shouldCreate_newUser() {
        // GIVEN a new user with unique email
        doReturn(Optional.empty())
                .when(userRepository).findByEmail(any(String.class));
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("ABCDEF123");
        doReturn(testUser)
                .when(userRepository).save(any(User.class));
        // WHEN
        testUser = userService.createUser("username@domain.com", "ABCDEF123");
        // THEN
        // asserting that created user is not null does not work, thus we check if the balance was actually set to
        // 0.00 during user creation
        verify(userRepository, times(1)).save(any(User.class));
        assertThat(testUser).isNotNull();
    }

    @Test
    @DisplayName("Updating user with valid email should update user")
    public void updateUser_usingValidEmail_shouldUpdate_user() {
        String emailAddress = "username@domain.com";
        testUser.setEmail(emailAddress);
        testUser.setBalance(new BigDecimal("3000.00"));
        doReturn(Optional.of(testUser))
                .when(userRepository).findByEmail(emailAddress);
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("ABCDEF123");
        doReturn(testUser)
                .when(userRepository).save(any(User.class));

        userService.updateUser(testUser);

        verify(userRepository, times(1)).save(testUser);
        assertThat(testUser.getBalance()).isEqualTo(new BigDecimal("3000.00"));
        assertTrue(testUser.getEmail().equalsIgnoreCase(emailAddress));
    }

    @Test
    @DisplayName("Updating user with invalid email should throw exception")
    public void updateUser_usingValidEmail_shouldThrow_exception() {
        String emailAddress = "username@domain";
        testUser.setEmail(emailAddress);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(testUser));
    }

    @Test
    @DisplayName("Updating a non-existing user should throw an exception")
    void updateUser_shouldThrowException_whenEmailNotFound() {
        when(userRepository.findByEmail(any(String.class)))
                .thenReturn(Optional.empty());
        // THEN
        assertThrows(BuddyNotFoundException.class, () -> userService.updateUser(testUser));
    }

    @Test
    @DisplayName("Updating user's lastname should update last name")
    void updateUser() {
        doReturn(Optional.of(testUser))
                .when(userRepository).findByEmail(any(String.class));
        String lastNameAfter = "Bing-Geller";
        testUser.setLastName(lastNameAfter);
        doReturn(testUser)
                .when(userRepository).save(testUser);

        testUser = userService.updateUser(testUser);
        // THEN
        assertTrue(testUser.getLastName().equalsIgnoreCase(lastNameAfter));
    }

    @Test
    @DisplayName("Saving a user with already existing email should throw exception")
    void createUser_shouldThrow_exception() {
        // GIVEN a new user with ready used email
        when(userRepository.findByEmail(any(String.class)))
                .thenReturn(Optional.of(testUser));
        // THEN
        assertThrows(EmailAlreadyUsedException.class, () -> userService.createUser("username@domain.com", "ABCDEF123"));
    }

    @Test
    @DisplayName("Deposit should add amount to user's balance")
    void deposit_shouldAdd_amount() {
        String amount = "490.44";
        userService.deposit(testUser, amount);
        assertThat(testUser.getBalance()).isEqualTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Deposit should replace any \"-\" in amount ")
    void deposit_shouldReplaceSign() {
        String amount = "-490.44";
        userService.deposit(testUser, amount);
        assertThat(testUser.getBalance()).isEqualTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Withdrawal should withdraw money from user's account")
    void withdraw_shouldWithdraw_amount() {
        String amount = "509.56";
        userService.withdraw(testUser, amount);
        assertThat(testUser.getBalance()).isEqualTo("2000.00");
    }

    @Test
    @DisplayName("Withdrawal should replace any \"-\" in amount ")
    void withdraw_shouldReplaceSign() {
        String amount = "-509.56";
        userService.withdraw(testUser, amount);
        assertThat(testUser.getBalance()).isEqualTo(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("getUsers should return a list of User with their email, first and last names, and balance " +
                 "information")
    void getUsers_shouldReturn_listOfUserViewModels() {
        when(userRepository.findAll()).thenReturn(List.of(testUser, otherUser));

        List<UserViewModel> result = userService.getUsers();

        assertTrue(result.get(0).getEmail().equalsIgnoreCase(testUser.getEmail()));
        assertTrue(result.get(0).getFirstname().equalsIgnoreCase(testUser.getFirstName()));
        assertTrue(result.get(0).getLastname().equalsIgnoreCase(testUser.getLastName()));
        assertThat(result.get(0).getBalance()).isEqualTo(testUser.getBalance());

        assertTrue(result.get(1).getEmail().equalsIgnoreCase(otherUser.getEmail()));
        assertTrue(result.get(1).getFirstname().equalsIgnoreCase(otherUser.getFirstName()));
        assertTrue(result.get(1).getLastname().equalsIgnoreCase(otherUser.getLastName()));
        assertThat(result.get(1).getBalance()).isEqualTo(otherUser.getBalance());
    }
}
