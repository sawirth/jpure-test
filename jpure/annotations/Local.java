package jpure.annotations;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Local {
	int value() default 1;
}
