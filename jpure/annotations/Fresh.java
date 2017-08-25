package jpure.annotations;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Fresh {
	int value() default Integer.MAX_VALUE;
}
