import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class G1GCDemo {
    public static void main(String[] args) {
        List<byte[]> objects = new ArrayList<>();
        Random random = new Random();

        long start = System.currentTimeMillis();
        long end = start + 60 * 1000 * 10; // 运行10分钟

        while (System.currentTimeMillis() < end) {
            if (random.nextBoolean()) {
                // 分配新的对象
                objects.add(new byte[random.nextInt(100 * 1024)]); // 分配随机大小的数组
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

        System.out.println("程序运行结束");
    }
}

