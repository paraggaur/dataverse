/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author skraffmi
 */
@Stateless
public class MyDataQueryHelperServiceBean {
    
    private DataverseRolePermissionHelper rolePermissionHelper;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DataverseRoleServiceBean roleService;
    
    @EJB
    DvObjectServiceBean dvObjectService;
    
    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    
    public Query getDirectQuery( AuthenticatedUser user) {
        return em.createNativeQuery("SELECT id FROM dvobject WHERE "
                + " id in (select definitionpoint_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "'));");
    }

    public List<Long> getParentIds(String dtypeParent, String dtypeChild, AuthenticatedUser user) {
        
        List<DataverseRole> roleList = roleService.findAll();

        DataverseRolePermissionHelper rolePermissionHelper = new DataverseRolePermissionHelper(roleList);
        List<Long> retVal = new ArrayList();
        List<Object[]> results = em.createNativeQuery("Select distinct role.role_id, dvo.id FROM dvobject dvo, roleassignment role WHERE  "
                + " dtype = '" + dtypeParent + "'"
                + " and dvo.id = role.definitionpoint_id"
                + " and role.role_id in (select role_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + ");").getResultList();
        for (Object[] result : results) {
            Long role_id = (Long) result[0];
            
            if (dtypeParent.equals("Dataverse") && dtypeChild.equals("Dataset") && rolePermissionHelper.hasDatasetPermissions(role_id)) {
                Integer r1 = (Integer) result[1];
                retVal.add(r1.longValue());
            }

            if (dtypeParent.equals("Dataverse") && dtypeChild.equals("DataFile") && rolePermissionHelper.hasFilePermissions(role_id)) {
                List<Object> dsIds = em.createNativeQuery("Select id from dvobject where dtype = 'Dataset' and owner_id = " + (Integer) result[1] + ";").getResultList();
                for (Object dsId : dsIds) {
                    Integer r1 = (Integer) dsId;
                    retVal.add(r1.longValue());
                }
            }

            if (dtypeParent.equals("Dataset")) {
                if (rolePermissionHelper.hasFilePermissions(role_id)) {
                    Integer r1 = (Integer) result[1];
                    retVal.add(r1.longValue());
                }
            }
        }
        return retVal;
    }
    
     private String getRoleIdListClause(List<Long> roleIdList){
        if (roleIdList == null){
            return "";
        }
        List<String> outputList = new ArrayList<>();
        
        for(Long r : roleIdList){
            if (r != null){
                outputList.add(r.toString());
            }
        }
        if (outputList.isEmpty()){
            return "";
        }        
        return " AND role.role_id IN (" + StringUtils.join(outputList, ",") + ")";        
    }
    
    public List<String> getRolesOnDVO(AuthenticatedUser user, Long dvoId, List<Long> roleIdList) {

        List<String> retVal = new ArrayList();

        DvObject objectIn = dvObjectService.findDvObject(dvoId);
        List idsForSelect = new ArrayList();
        /*
        for (Long roleId : roleIdList) {

            if (objectIn.isInstanceofDataverse() &&   this.rolePermissionHelper.hasDataversePermissions(roleId)){
                idsForSelect.add(roleId);
            }
            if (objectIn.isInstanceofDataset() &&   this.rolePermissionHelper.hasDatasetPermissions(roleId)){
                idsForSelect.add(roleId);
            }
            if (objectIn.isInstanceofDataFile() &&  this.rolePermissionHelper.hasFilePermissions(roleId)){
                idsForSelect.add(roleId);
            }

        }*/
        String roleClause = getRoleIdListClause(roleIdList);
        
        
        List<Object> results = em.createNativeQuery("Select distinct role.role_id FROM roleassignment role WHERE  "
                + " role.definitionpoint_id = " + dvoId + " "
                + " and role.role_id in (select role_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + ")"
                + roleClause
                + ";").getResultList();
        if (results != null && !results.isEmpty()) {
            for (Object result : results) {
                Long role_id = (Long) result;
                DataverseRole role = roleService.find(role_id);
                if (!retVal.contains(role.getName())) {
                    retVal.add(role.getName());
                }
            }
        }
        Object parentObj = em.createNativeQuery("Select owner_id from dvobject where id = " + dvoId).getSingleResult();
        Long parentId = (Long) parentObj;
        List<Object> resultsParent = em.createNativeQuery("Select distinct role.role_id FROM roleassignment role WHERE  "
                + " role.definitionpoint_id = " + parentId + " "
                + " and role.role_id in (select role_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + ")"
                + roleClause
                + ";").getResultList();
        if (resultsParent != null && !resultsParent.isEmpty()) {
            for (Object result : resultsParent) {
                Long role_id = (Long) result;
                DataverseRole role = roleService.find(role_id);
                if (!retVal.contains(role.getName())) {
                    retVal.add(role.getName());
                }
            }
        }
        Object GrandParentObj = em.createNativeQuery("Select owner_id from dvobject where id = " + parentId).getSingleResult();
        Long grandParentId = (Long) GrandParentObj;
        List<Object> resultsGrandParent = em.createNativeQuery("Select distinct role.role_id FROM roleassignment role WHERE  "
                + " role.definitionpoint_id = " + grandParentId + " "
                + " and role.role_id in (select role_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + ")"
                + roleClause
                + ";").getResultList();
        if (resultsGrandParent != null && !resultsGrandParent.isEmpty()) {
            for (Object result : resultsGrandParent) {
                Long role_id = (Long) result;
                DataverseRole role = roleService.find(role_id);
                if (!retVal.contains(role.getName())) {
                    retVal.add(role.getName());
                }
            }
        }

        return retVal;
    }

}
