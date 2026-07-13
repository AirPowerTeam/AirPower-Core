package cn.hamm.airpower.core;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>CollectionUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class CollectionUtilTest {

    // ==================== getCollectWithoutNull 方法测试 ====================

    @Test
    void testGetCollectWithoutNullWithNullList() {
        Collection<String> result = CollectionUtil.getCollectWithoutNull(null, ArrayList.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertInstanceOf(ArrayList.class, result);
    }

    @Test
    void testGetCollectWithoutNullWithNullSet() {
        Collection<String> result = CollectionUtil.getCollectWithoutNull(null, Set.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertInstanceOf(HashSet.class, result);
    }

    @Test
    void testGetCollectWithoutNullWithNonNullList() {
        List<String> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");
        Collection<String> result = CollectionUtil.getCollectWithoutNull(list, ArrayList.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(list, result);
    }

    @Test
    void testGetCollectWithoutNullWithNonNullSet() {
        Set<String> set = new HashSet<>();
        set.add("item1");
        Collection<String> result = CollectionUtil.getCollectWithoutNull(set, Set.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(set, result);
    }

    @Test
    void testGetCollectWithoutNullWithEmptyList() {
        List<String> list = new ArrayList<>();
        Collection<String> result = CollectionUtil.getCollectWithoutNull(list, ArrayList.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertSame(list, result);
    }

    @Test
    void testGetCollectWithoutNullWithEmptySet() {
        Set<String> set = new HashSet<>();
        Collection<String> result = CollectionUtil.getCollectWithoutNull(set, Set.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertSame(set, result);
    }

    @Test
    void testGetCollectWithoutNullWithLinkedList() {
        // 传入非 Set 类型的 List，应该返回 ArrayList
        List<String> list = new LinkedList<>();
        list.add("item");
        Collection<String> result = CollectionUtil.getCollectWithoutNull(list, LinkedList.class);
        assertNotNull(result);
        assertSame(list, result);
    }

    @Test
    void testGetCollectWithoutNullWithTreeSet() {
        // 传入 Set 类型
        Set<String> set = new TreeSet<>();
        set.add("item");
        Collection<String> result = CollectionUtil.getCollectWithoutNull(set, Set.class);
        assertNotNull(result);
        assertSame(set, result);
    }

    @Test
    void testGetCollectWithoutNullWithNullAndWrongClass() {
        // 传入 null 和不为 Set 的 class，应该返回 ArrayList
        Collection<String> result = CollectionUtil.getCollectWithoutNull(null, LinkedList.class);
        assertNotNull(result);
        assertInstanceOf(ArrayList.class, result);
    }
}
