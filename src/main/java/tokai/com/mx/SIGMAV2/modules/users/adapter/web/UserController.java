package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;
import tokai.com.mx.SIGMAV2.modules.users.port.in.UserService;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserResponse;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;


@RestController
@RequestMapping("/api/users")
public class UserController {


private final UserService userService;


public UserController(UserService userService) {
this.userService = userService;
}


@PostMapping
public ResponseEntity<Object> createUser(@RequestBody UserRequest request) {
BeanUser user = userService.createUser(request.toDomain());
return ResponseEntity.ok(UserResponse.fromDomain(user));
}
    
}
