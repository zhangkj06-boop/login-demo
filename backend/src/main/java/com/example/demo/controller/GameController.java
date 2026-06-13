package com.example.demo.controller;

import com.example.demo.entity.Game;
import com.example.demo.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏控制器。
 * <p>
 * 提供游戏管理的完整 REST API，包括增删改查。
 * </p>
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    /** 游戏数据访问接口 */
    private final GameRepository gameRepository;

    public GameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * 查询所有游戏列表（按排序号升序）。
     *
     * @return 游戏列表
     */
    @GetMapping
    public ResponseEntity<List<Game>> listGames() {
        logger.info("查询所有游戏列表");
        List<Game> games = gameRepository.findAllByOrderBySortOrderAsc();
        logger.info("查询游戏列表成功，共 {} 条记录", games.size());
        return ResponseEntity.ok(games);
    }

    /**
     * 查询所有已上线的游戏（按排序号升序）。
     *
     * @return 已上线的游戏列表
     */
    @GetMapping("/online")
    public ResponseEntity<List<Game>> listOnlineGames() {
        logger.info("查询已上线游戏列表");
        List<Game> games = gameRepository.findByStatusOrderBySortOrderAsc("上线");
        logger.info("查询已上线游戏列表成功，共 {} 条记录", games.size());
        return ResponseEntity.ok(games);
    }

    /**
     * 根据ID查询游戏详情。
     *
     * @param id 游戏主键
     * @return 游戏详情；不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGame(@PathVariable Long id) {
        logger.info("查询游戏详情，ID: {}", id);
        return gameRepository.findById(id)
                .map(game -> {
                    logger.info("查询游戏详情成功，ID: {}", id);
                    return ResponseEntity.ok(game);
                })
                .orElseGet(() -> {
                    logger.warn("查询游戏详情失败，游戏不存在，ID: {}", id);
                    return ResponseEntity.status(404).body((Game) null);
                });
    }

    /**
     * 创建新游戏。
     *
     * @param game 游戏实体
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createGame(@RequestBody Game game) {
        logger.info("创建游戏，名称: {}, 编码: {}", game.getName(), game.getCode());
        Map<String, Object> result = new HashMap<>();
        if (game.getName() == null || game.getName().isBlank()) {
            logger.warn("创建游戏失败，名称不能为空");
            result.put("success", false);
            result.put("message", "游戏名称不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (game.getCode() == null || game.getCode().isBlank()) {
            logger.warn("创建游戏失败，编码不能为空");
            result.put("success", false);
            result.put("message", "游戏编码不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (gameRepository.existsByCode(game.getCode())) {
            logger.warn("创建游戏失败，编码已存在: {}", game.getCode());
            result.put("success", false);
            result.put("message", "游戏编码已存在");
            return ResponseEntity.badRequest().body(result);
        }
        if (game.getStatus() == null || game.getStatus().isBlank()) {
            game.setStatus("上线");
        }
        Game saved = gameRepository.save(game);
        logger.info("创建游戏成功，ID: {}", saved.getId());
        result.put("success", true);
        result.put("message", "创建成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新游戏信息（部分更新）。
     *
     * @param id   游戏主键
     * @param game 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGame(@PathVariable Long id, @RequestBody Game game) {
        logger.info("更新游戏，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        Game existing = gameRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新游戏失败，游戏不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "游戏不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (game.getName() != null && !game.getName().isBlank()) {
            existing.setName(game.getName());
        }
        if (game.getCode() != null && !game.getCode().isBlank()
                && !game.getCode().equals(existing.getCode())) {
            if (gameRepository.existsByCode(game.getCode())) {
                logger.warn("更新游戏失败，编码已存在: {}", game.getCode());
                result.put("success", false);
                result.put("message", "游戏编码已存在");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setCode(game.getCode());
        }
        if (game.getDescription() != null) {
            existing.setDescription(game.getDescription());
        }
        if (game.getIcon() != null) {
            existing.setIcon(game.getIcon());
        }
        if (game.getStatus() != null && !game.getStatus().isBlank()) {
            existing.setStatus(game.getStatus());
        }
        if (game.getSortOrder() != null) {
            existing.setSortOrder(game.getSortOrder());
        }
        gameRepository.save(existing);
        logger.info("更新游戏成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除游戏。
     *
     * @param id 游戏主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGame(@PathVariable Long id) {
        logger.info("删除游戏，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        if (!gameRepository.existsById(id)) {
            logger.warn("删除游戏失败，游戏不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "游戏不存在");
            return ResponseEntity.status(404).body(result);
        }
        gameRepository.deleteById(id);
        logger.info("删除游戏成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
