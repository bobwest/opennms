/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest.v2;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opennms.netmgt.bsm.service.BusinessServiceManager;
import org.opennms.netmgt.bsm.service.model.BusinessServiceDTO;
import org.opennms.netmgt.model.OnmsSeverity;
import org.opennms.web.rest.api.ResourceLocationFactory;
import org.opennms.web.rest.support.RedirectHelper;
import org.opennms.web.rest.v2.bsm.model.BusinessServiceListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Path("business-services")
@Transactional
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
public class BusinessServiceRestService {

    @Autowired
    private BusinessServiceManager businessServiceManager;

    protected BusinessServiceManager getManager() {
        return businessServiceManager;
    }

    @GET
    public Response list() {
        List<BusinessServiceDTO> services = getManager().findAll();
        if (services == null || services.isEmpty()) {
            return Response.noContent().build();
        }
        BusinessServiceListDTO serviceList = new BusinessServiceListDTO(services, ResourceLocationFactory.createBusinessServiceLocation());
        return Response.ok(serviceList).build();
    }

    @GET
    @Path("{id}")
    public Response getById(@PathParam("id") Long id) {
        BusinessServiceDTO entity = getManager().getById(id);
        return Response.ok(entity).build();
    }

    @POST
    public Response create(@Context final UriInfo uriInfo, BusinessServiceDTO objectToCreate) {
        Long id = getManager().save(objectToCreate);
        return Response.created(RedirectHelper.getRedirectUri(uriInfo, id)).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) {
        getManager().delete(id);
        return Response.ok().build();
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") Long id, BusinessServiceDTO dto) {
        if (!id.equals(dto.getId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        getManager().update(dto);
        return Response.noContent().build();
    }

    @GET
    @Path("{id}/operational-status")
    public Response getOperationStatusForBusinessServiceById(@PathParam("id") Long id) {
        final OnmsSeverity severity = getManager().getOperationalStatusForBusinessService(id);
        if (severity != null) {
            return Response.ok(severity.toString()).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("{id}/ip-service/{ipServiceId}")
    public Response attachIpService(@PathParam("id") final Long serviceId,
                                    @PathParam("ipServiceId") final Integer ipServiceId) {
        boolean changed = getManager().assignIpInterface(serviceId, ipServiceId);
        if (!changed) {
            return Response.notModified().build();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("{id}/ip-service/{ipServiceId}")
    public Response detachIpService(@PathParam("id") final Long serviceId,
                                    @PathParam("ipServiceId") final Integer ipServiceId) {
        boolean changed = getManager().removeIpInterface(serviceId, ipServiceId);
        if (!changed) {
            return Response.notModified().build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("{id}/ip-service/operational-status/{ipServiceId}")
    public Response getOperationStatusForIPServiceById(@PathParam("id") Integer ipServiceId) {
        final OnmsSeverity severity = getManager().getOperationalStatusForIPService(ipServiceId);
        if (severity != null) {
            return Response.ok(severity.toString()).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.noContent().build();
    }
}