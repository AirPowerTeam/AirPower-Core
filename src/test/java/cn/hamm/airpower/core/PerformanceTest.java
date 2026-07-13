package cn.hamm.airpower.core;

import cn.hamm.airpower.core.interfaces.IEntity;
import cn.hamm.airpower.core.interfaces.ITree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>性能测试</h1>
 *
 * @author Hamm.cn
 */
class PerformanceTest {

    /**
     * 测试 TreeUtil 构建树结构的性能
     */
    @Test
    void testTreeBuildPerformance() {
        int[] sizes = {100, 1000, 5000, 10000};
        for (int size : sizes) {
            List<TestTreeNode> nodes = generateTreeNodes(size);
            long start = System.nanoTime();
            List<TestTreeNode> tree = TreeUtil.buildTreeList(nodes);
            long end = System.nanoTime();
            long ms = TimeUnit.NANOSECONDS.toMillis(end - start);
            System.out.printf("TreeUtil.buildTreeList - %d nodes: %d ms%n", size, ms);
            assertFalse(tree.isEmpty(), "树不应为空");
        }
    }

    /**
     * 测试 DateTimeUtil.format 的性能（带缓存 vs 不带缓存）
     */
    @Test
    void testDateTimeFormatPerformance() {
        int iterations = 100000;
        long timestamp = System.currentTimeMillis();

        // 预热
        for (int i = 0; i < 1000; i++) {
            DateTimeUtil.format(timestamp, "yyyy-MM-dd HH:mm:ss");
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DateTimeUtil.format(timestamp, "yyyy-MM-dd HH:mm:ss");
        }
        long end = System.nanoTime();
        long ms = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.printf("DateTimeUtil.format - %d iterations: %d ms (%.2f ops/ms)%n",
                iterations, ms, (double) iterations / ms);
        assertTrue(ms < 5000, "格式化应在合理时间内完成");
    }

    /**
     * 测试 ReflectUtil.getFieldList 的性能
     */
    @Test
    void testReflectFieldListPerformance() {
        int iterations = 10000;

        // 预热
        for (int i = 0; i < 100; i++) {
            ReflectUtil.getFieldList(TestModel.class);
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ReflectUtil.getFieldList(TestModel.class);
        }
        long end = System.nanoTime();
        long ms = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.printf("ReflectUtil.getFieldList - %d iterations: %d ms (%.2f ops/ms)%n",
                iterations, ms, (double) iterations / ms);
        assertTrue(ms < 5000, "反射应在合理时间内完成");
    }

    /**
     * 测试 DesensitizeUtil.replace 的性能
     */
    @Test
    void testDesensitizePerformance() {
        int iterations = 100000;
        String text = "13800138000"; // 手机号

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DesensitizeUtil.replace(text, 3, 4, "*");
        }
        long end = System.nanoTime();
        long ms = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.printf("DesensitizeUtil.replace - %d iterations: %d ms (%.2f ops/ms)%n",
                iterations, ms, (double) iterations / ms);
        assertTrue(ms < 5000, "脱敏应在合理时间内完成");
    }

    /**
     * 测试 RandomUtil.randomBytes 的性能
     */
    @Test
    void testRandomBytesPerformance() {
        int iterations = 10000;

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            RandomUtil.randomBytes(32);
        }
        long end = System.nanoTime();
        long ms = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.printf("RandomUtil.randomBytes - %d iterations: %d ms (%.2f ops/ms)%n",
                iterations, ms, (double) iterations / ms);
        assertTrue(ms < 5000, "随机字节生成应在合理时间内完成");
    }

    /**
     * 测试 AES 加密/解密性能
     */
    @Test
    void testAesPerformance() {
        int iterations = 1000;
        AesUtil aes = AesUtil.create()
                .setKey("1234567890123456")
                .setIv("0000000000000000");
        String plainText = "Hello, World! This is a test message for AES encryption performance.";

        // 预热
        for (int i = 0; i < 10; i++) {
            String encrypted = aes.encrypt(plainText);
            aes.decrypt(encrypted);
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String encrypted = aes.encrypt(plainText);
            aes.decrypt(encrypted);
        }
        long end = System.nanoTime();
        long ms = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.printf("AesUtil encrypt/decrypt - %d iterations: %d ms (%.2f ops/ms)%n",
                iterations, ms, (double) iterations / ms);
        assertTrue(ms < 5000, "AES 加解密应在合理时间内完成");
    }

    /**
     * 测试并发场景下的 Json 序列化性能
     */
    @Test
    void testJsonConcurrentPerformance() throws InterruptedException {
        int threads = Runtime.getRuntime().availableProcessors();
        int iterationsPerThread = 10000;
        TestModel model = new TestModel();
        model.setName("Test");
        model.setAge(25);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        long[] times = new long[threads];

        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            executor.submit(() -> {
                long start = System.nanoTime();
                for (int i = 0; i < iterationsPerThread; i++) {
                    Json.toString(model);
                }
                times[threadIndex] = System.nanoTime() - start;
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        long totalTime = 0;
        for (long time : times) {
            totalTime += time;
        }
        long avgMs = TimeUnit.NANOSECONDS.toMillis(totalTime / threads);
        int totalOps = threads * iterationsPerThread;
        System.out.printf("Json.toString concurrent - %d threads x %d ops: avg %d ms/thread (%.2f ops/ms)%n",
                threads, iterationsPerThread, avgMs, (double) totalOps / (avgMs * threads));
        assertTrue(avgMs < 5000, "JSON 序列化应在合理时间内完成");
    }

    // ==================== 辅助方法 ====================

    private List<TestTreeNode> generateTreeNodes(int count) {
        List<TestTreeNode> nodes = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 1; i <= count; i++) {
            TestTreeNode node = new TestTreeNode();
            node.setId((long) i);
            // 随机分配父节点（确保父节点存在）
            long parentId = i == 1 ? 0 : random.nextInt(i - 1) + 1;
            node.setParentId(parentId);
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * 测试树节点
     */
    static class TestTreeNode implements IEntity<TestTreeNode>, ITree<TestTreeNode> {
        private Long id;
        private Long parentId;
        private List<TestTreeNode> children;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public TestTreeNode setId(Long id) {
            this.id = id;
            return this;
        }

        @Override
        public Long getParentId() {
            return parentId;
        }

        @Override
        public TestTreeNode setParentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        @Override
        public List<TestTreeNode> getChildren() {
            return children;
        }

        @Override
        public TestTreeNode setChildren(List<TestTreeNode> children) {
            this.children = children;
            return this;
        }
    }

    /**
     * 测试模型
     */
    static class TestModel extends RootModel<TestModel> {
        private String name;
        private int age;
        private String email;
        private String phone;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}
