package com.cutlass.confluence.plugin.macro;

import java.util.Map;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.user.User;
import com.cutlass.confluence.plugin.rest.HashTranslator;


public class CasDebugListMacro extends BaseMacro
{

    PermissionManager permissionManager;

    public String execute(final Map arg0, final String arg1, final RenderContext arg2) throws MacroException
    {

        final User remoteUser = AuthenticatedUserThreadLocal.getUser();

        final Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        final HashTranslator translator = HashTranslator.getInstance();

        contextMap.put("admin", permissionManager.isConfluenceAdministrator(remoteUser));
        contextMap.put("userMap", translator.getTranslation());
        contextMap.put("username", remoteUser.getName());
        contextMap.put("emailhash", translator.getEmailHash(remoteUser.getEmail()));

        return VelocityUtils.getRenderedTemplate("templates/casdebuglistmacro.vm", contextMap);
    }

    public RenderMode getBodyRenderMode()
    {
        return RenderMode.NO_RENDER;
    }

    public boolean hasBody()
    {
        return false;
    }

    public void setPermissionManager(final PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

}
