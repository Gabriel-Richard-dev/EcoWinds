package br.com.ecowinds.secutiry;

import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.repository.EspDeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class DeviceApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Device-Key";
    public static final String ROLE = "ROLE_DEVICE";

    private final EspDeviceRepository deviceRepository;

    public DeviceApiKeyFilter(EspDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/devices/me");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        if (rawKey != null && !rawKey.isBlank()) {
            String hash = ApiKeyHasher.sha256(rawKey.trim());
            EspDevice device = deviceRepository.findByApiKeyHash(hash).orElse(null);
            if (device != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                        device.getId(), null,
                        List.of(new SimpleGrantedAuthority(ROLE)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
