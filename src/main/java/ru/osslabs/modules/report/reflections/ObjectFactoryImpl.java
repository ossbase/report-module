package ru.osslabs.modules.report.reflections;

import javaslang.collection.Stream;
import javaslang.control.Try;
import ru.osslabs.model.datasource.DataObject;
import ru.osslabs.model.datasource.DataObjectField;
import ru.osslabs.model.datasource.MetaObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static ru.osslabs.modules.report.reflections.ObjectUtils.*;

/**
 * Created by ikuchmin on 24.11.15.
 */
public class ObjectFactoryImpl<T> extends AbstractObjectFactory<T> {

    private static Logger log = Logger.getLogger(ObjectFactoryImpl.class.getName());

    public ObjectFactoryImpl(ObjectRegistry objectRegistry) {
        super(objectRegistry);
    }

    /**
     * I should check this builder on Parametrized Types
     *
     * @param dataObject
     * @param typeRef
     * @return
     */
    @Override
    public T build(DataObject dataObject, Supplier<MetaObject> fnMetaObject, ReferenceSupplier<? extends T> typeRef) {

        Class<?> rawType = getRawType(typeRef);

        Object instance;
        try {
            instance = rawType.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Constructor without arguments not found", e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Instance don't get from constructor", e);
        }

        Map<String, DataObjectField> dFields = dataObject.getFields();
        Try.of(() -> rawType.getDeclaredField("id"))
                .onFailure(e -> log.info(() -> "Field id does not exist"))
                .andThen(f -> {
                    f.setAccessible(true);
                    f.set(instance, Integer.parseInt(dataObject.getId()));
                })
                .onFailure(e -> log.warning(() -> "Field with name "
                        .concat("id isn't set")
                        .concat(" Factory type: String.")
                        .concat(" Parent object description in CMDBuild: ").concat(dFields.get("Description").getValue().toString()).concat(".")
                        .concat(" Message error: ").concat(e.getMessage())));

        Stream.ofAll(rawType.getDeclaredFields()).forEach(f ->
                Try.of(() -> objectRegistry.dispatch(f.getType()))
                        .filter(v -> v != null)
                        .onFailure((e) -> log.warning(() -> "Function dispatcher for object with type ".concat(f.getType().getTypeName())
                                .concat(" not found.")
                                .concat(" Message: ").concat(e.getMessage())))
                        .flatMap(fn -> fn.apply(dFields.get(actualFieldName(f)), f::getGenericType))
                        .andThen(v -> {
                            f.setAccessible(true);
                            f.set(instance, v);
                        })
                        .onFailure(e -> log.warning(() -> "Field with name "
                                .concat(f.getName()).concat(" isn't set")
                                .concat(" Factory type: ").concat(f.getGenericType().getTypeName()).concat(".")
                                .concat(" Parent object description in CMDBuild: ").concat(dFields.get("Description").getValue().toString()).concat(".")
                                .concat(" Message error: ").concat(e.getMessage()))));

        return cast(instance);
    }

    @Override
    public T build(List<DataObject> dataObjectList, Supplier<MetaObject> fnMetaObject, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }

    @Override
    public T build(DataObject[] dataObjects, Supplier<MetaObject> fnMetaObject, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }

    @Override
    public T build(String dataObject, Supplier<MetaObject> fnMetaObject, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }

    @Override
    public T build(Supplier<MetaObject> fnMetaObject, ReferenceSupplier<? extends T> typeRef) {
        return null;
    }


}
