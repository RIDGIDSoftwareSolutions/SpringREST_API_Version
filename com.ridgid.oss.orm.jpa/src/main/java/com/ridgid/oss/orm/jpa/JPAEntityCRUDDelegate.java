package com.ridgid.oss.orm.jpa;

import com.ridgid.oss.common.hierarchy.GeneralVisitHandler;
import com.ridgid.oss.common.hierarchy.HierarchyProcessor;
import com.ridgid.oss.common.hierarchy.HierarchyProcessor.Traversal;
import com.ridgid.oss.common.hierarchy.VisitStatus;
import com.ridgid.oss.orm.EntityCRUD;
import com.ridgid.oss.orm.entity.PrimaryKeyedEntity;
import com.ridgid.oss.orm.exception.EntityCRUDExceptionError;
import com.ridgid.oss.orm.jpa.exception.EntityManagerNullException;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ridgid.oss.common.hierarchy.HierarchyProcessor.Traversal.BREADTH_FIRST;
import static com.ridgid.oss.common.hierarchy.HierarchyProcessor.Traversal.DEPTH_FIRST;

@SuppressWarnings({"WeakerAccess", "unused"})
final class JPAEntityCRUDDelegate<ET extends PrimaryKeyedEntity<PKT>, PKT extends Comparable<PKT>>
    implements JPAEntityCRUDDelegateRequired<ET, PKT>
{

    public final String     PK_NAME;
    public final Class<ET>  classType;
    public final Class<PKT> pkType;
    public final short      loadBatchSize;

    EntityManager entityManager;

    public JPAEntityCRUDDelegate(Class<ET> classType,
                                 Class<PKT> pkType)
    {
        this.classType     = classType;
        this.pkType        = pkType;
        this.PK_NAME       = "pk";
        this.loadBatchSize = 1000;
    }

    public JPAEntityCRUDDelegate(Class<ET> classType,
                                 Class<PKT> pkType,
                                 String pkName)
    {
        this.classType     = classType;
        this.pkType        = pkType;
        this.PK_NAME       = pkName;
        this.loadBatchSize = 1000;
    }

    public JPAEntityCRUDDelegate(Class<ET> classType,
                                 Class<PKT> pkType,
                                 short loadBatchSize)
    {
        this.classType     = classType;
        this.pkType        = pkType;
        this.PK_NAME       = "pk";
        this.loadBatchSize = loadBatchSize;
    }

    public JPAEntityCRUDDelegate(Class<ET> classType,
                                 Class<PKT> pkType,
                                 String pkName,
                                 short loadBatchSize)
    {
        this.classType     = classType;
        this.pkType        = pkType;
        this.PK_NAME       = pkName;
        this.loadBatchSize = loadBatchSize;
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public final EntityManager getEntityManager() {
        return entityManager;
    }

    public Class<ET> getClassType() {
        return classType;
    }

    public Class<PKT> getPkType() {
        return pkType;
    }

    @Override
    public final ET initializeAndDetach(ET entity,
                                        HierarchyProcessor<ET> hierarchy)
    {
        visitEntityHierarchy
            (
                entity,
                hierarchy,
                this::initializeEntityVisitHandler,
                this::detachEntityVisitHandler,
                BREADTH_FIRST
            );
        return entity;
    }

    @Override
    public final Optional<ET> load(PKT pk) {
        return Optional.ofNullable
            (
                entityManager.find(classType, pk)
            );
    }

    @Override
    public final Stream<ET> loadBatch(List<PKT> pkList) {
        return entityManager
            .createQuery(getEntitiesForPrimaryKeysQuery())
            .setParameter("searchKeys", pkList)
            .getResultStream();
    }

    private CriteriaQuery<ET> getEntitiesForPrimaryKeysQuery() {
        CriteriaBuilder           builder    = entityManager.getCriteriaBuilder();
        CriteriaQuery<ET>         cQuery     = builder.createQuery(classType);
        Root<ET>                  entity     = cQuery.from(classType);
        Path<String>              pk         = entity.get(PK_NAME);
        ParameterExpression<List> searchKeys = builder.parameter(List.class, "searchKeys");
        cQuery.select(entity).where(pk.in(searchKeys));
        return cQuery;
    }

    @Override
    public final short getLoadBatchSize() {
        return loadBatchSize;
    }

    @SuppressWarnings("TypeParameterHidesVisibleType")
    public final <ET> ET initializeEntity(ET entity,
                                          HierarchyProcessor<ET> hierarchy)
    {
        visitEntityHierarchy
            (
                entity,
                hierarchy,
                this::initializeEntityVisitHandler,
                null,
                BREADTH_FIRST
            );
        return entity;
    }

    @SuppressWarnings("TypeParameterHidesVisibleType")
    public final <ET> ET detachEntity(ET entity,
                                      HierarchyProcessor<ET> hierarchy)
    {
        visitEntityHierarchy
            (
                entity,
                hierarchy,
                EntityCRUD.NO_OP_VISIT_HANDLER,
                this::detachEntityVisitHandler,
                DEPTH_FIRST
            );
        return entity;
    }

    @Override
    public void flushContext() {
        entityManager.flush();
    }

    @Override
    public void clearContext() {
        entityManager.clear();
    }

    final RuntimeException enhanceExceptionWithEntityManagerNullCheck(Exception e) {
        if ( entityManager == null )
            return new EntityManagerNullException(e);
        else
            return new EntityCRUDExceptionError(e);
    }

    @SuppressWarnings("TypeParameterHidesVisibleType")
    private <ET> void visitEntityHierarchy(ET entity,
                                           HierarchyProcessor<ET> hierarchy,
                                           GeneralVisitHandler visitor,
                                           GeneralVisitHandler afterChildrenVisitor,
                                           Traversal traversal)
    {
        try {
            if ( hierarchy == null ) {
                visitEntity
                    (
                        entity,
                        visitor,
                        afterChildrenVisitor
                    );
                return;
            }
            hierarchy.visit
                (
                    entity,
                    visitor,
                    afterChildrenVisitor,
                    traversal
                );
        } catch ( Exception e ) {
            throw enhanceExceptionWithEntityManagerNullCheck(e);
        }
    }

    @SuppressWarnings("TypeParameterHidesVisibleType")
    private <ET> void visitEntity(ET entity,
                                  GeneralVisitHandler visitor,
                                  GeneralVisitHandler afterChildrenVisitor)
    {
        visitor.handle(null, entity);
        if ( afterChildrenVisitor != null )
            afterChildrenVisitor.handle(null, entity);
    }

    @SuppressWarnings("unused")
    private VisitStatus initializeEntityVisitHandler(Object p, Object o) {
        Hibernate.initialize(o);
        return VisitStatus.OK_CONTINUE;
    }

    @SuppressWarnings("unused")
    private VisitStatus detachEntityVisitHandler(Object p, Object o) {
        if ( o instanceof PrimaryKeyedEntity ) entityManager.detach(o);
        return VisitStatus.OK_CONTINUE;
    }
}
