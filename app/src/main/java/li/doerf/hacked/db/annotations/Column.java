package li.doerf.hacked.db.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by moo on 29/01/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    boolean isPrimaryKey() default false;
    boolean isAutoincrement() default false;
    boolean isReference() default false;
    /** Column type. */
    String type() default "TEXT";
    /** Column name. */
    String name();
}
