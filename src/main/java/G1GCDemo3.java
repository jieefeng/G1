import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class G1GCDemo3 {
    private static final List<byte[]> longLivedObjects = new ArrayList<>();
    private static final int NUM_LARGE_OBJECTS = 50;
    private static final int LARGE_OBJECT_SIZE = 2048 * 1024; // 2 MB

    public static void main(String[] args) throws Exception {

        Random random = new Random();

        List<byte[]> objects = new ArrayList<>();

        long start = System.currentTimeMillis();

        long end = start + 60 * 1000 * 10; // 运行10分钟

        // 分配一些长生命周期的大对象
        for (int i = 0; i < NUM_LARGE_OBJECTS; i++) {
            longLivedObjects.add(new byte[LARGE_OBJECT_SIZE]);
        }

        // 确保每次循环都创建一定数量的大对象
        while (System.currentTimeMillis() < end) {
            if (random.nextBoolean()) {
                // 确保分配一定数量的大对象
                for (int i = 0; i < 10; i++) {
                    longLivedObjects.add(new byte[LARGE_OBJECT_SIZE]);
                }
                // 分配随机大小的数组
                objects.add(new byte[random.nextInt(100 * 1024)]);
            } else if (!objects.isEmpty()) {
                // 释放一些对象
                objects.remove(random.nextInt(objects.size()));
            }

            // 短暂休眠以防止CPU过高
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println(longLivedObjects);

        System.out.println("a");
    }
}
