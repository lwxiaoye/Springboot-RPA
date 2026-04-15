package rpa.controller;

import rpa.entity.Credential;
import rpa.service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 凭据控制器
 * <p>
 * 提供凭据的CRUD接口，供前端和任务调度使用。
 * </p>
 */
@RestController
@RequestMapping("/api/credential")
@CrossOrigin(origins = "*")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    /**
     * 获取凭据列表（不解密）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listCredentials() {
        try {
            List<Credential> credentials = credentialService.listCredentials();
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", credentials
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取凭据详情（不解密）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCredential(@PathVariable Long id) {
        try {
            return credentialService.getCredentialById(id)
                .map(cred -> ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", cred
                )))
                .orElse(ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", "凭据不存在"
                )));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 创建凭据
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCredential(@RequestBody Credential credential) {
        try {
            Credential created = credentialService.createCredential(credential);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "创建成功",
                "data", created
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新凭据
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCredential(@PathVariable Long id, @RequestBody Credential credential) {
        try {
            Credential updated = credentialService.updateCredential(id, credential);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "更新成功",
                "data", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除凭据
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCredential(@PathVariable Long id) {
        try {
            credentialService.deleteCredential(id);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 搜索凭据
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCredentials(@RequestParam String keyword) {
        try {
            List<Credential> credentials = credentialService.searchCredentials(keyword);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", credentials
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 根据类型获取凭据
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<Map<String, Object>> getByType(@PathVariable String type) {
        try {
            List<Credential> credentials = credentialService.getCredentialsByType(type);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", credentials
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", e.getMessage()
            ));
        }
    }
}