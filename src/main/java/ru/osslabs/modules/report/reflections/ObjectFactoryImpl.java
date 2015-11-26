package ru.osslabs.modules.report.reflections;

import javaslang.control.Option;
import javaslang.control.Try;
import ru.osslabs.model.datasource.DataObject;
import ru.osslabs.model.datasource.DataObjectField;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static ru.osslabs.modules.report.reflections.ObjectUtils.actualFieldName;
import static ru.osslabs.modules.report.reflections.ObjectUtils.cast;

/**
 * Created by ikuchmin on 24.11.15.
 */
public class ObjectFactoryImpl<T> extends AbstractObjectFactory<T> {

    public ObjectFactoryImpl(ObjectRegistry objectRegistry) {
        super(objectRegistry);
    }

    /**
     * I should check this builder on Parametrized Types
     * @param dataObject
     * @param typeRef
     * @return
     */
    @Override
    public T build(DataObject dataObject, ReferenceSupplier<? extends T> typeRef) {
        Type type = typeRef.getType();
        Class<?> rawType = type instanceof Class<?>
                ? (Class<?>) type
                : (Class<?>) ((ParameterizedType) type).getRawType();

        Object instance;
        try {
            instance = rawType.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Constructor without arguments not found", e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Instance don't get from constructor", e);
        }

        for (Field field : rawType.getDeclaredFields()) {
            field.setAccessible(true);
            Map<String, DataObjectField> dFields = dataObject.getFields();
            Option.of(objectRegistry.dispatch(field.getType())) // TODO: При поиске метода построения объекта необходимо использовать его иерархию. По аналогии с бинами
                    .map((func) -> func.apply(dFields.get(actualFieldName(field)), field::getGenericType))
                    .peek((val) -> val.peek((v) -> Try.run(() -> field.set(instance, v)).onFailure(e -> {
                        throw new RuntimeException("Field with name " + field.getName() + " isn't set", e);
                    })));
        }
        return cast(instance);
    }

    @Override
    public T build(List<DataObject> dataObjectList, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }

    @Override
    public T build(DataObject[] dataObjects, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }

    @Override
    public T build(Object dataObjects, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }


}
