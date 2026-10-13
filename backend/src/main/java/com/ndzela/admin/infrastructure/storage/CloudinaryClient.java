package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.common.exception.StorageUnavailableException;
import com.ndzela.admin.config.properties.StorageProperties;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

// Talks to Cloudinary over its signed REST API.
// Private files are "authenticated" assets: nothing can fetch them without a request signed with the API secret, so they are read here and streamed after the caller's permission is checked.
// Public files (profile pictures) are ordinary CDN images.
public class CloudinaryClient {
    private static final String API = "https://api.cloudinary.com/v1_1/";
    private static final String AUTHENTICATED = "authenticated";

    private final RestClient client;
    private final StorageProperties props;
    private final ObjectMapper mapper;

    public CloudinaryClient(RestClient client, StorageProperties props, ObjectMapper mapper) {
        this.client = client;
        this.props = props;
        this.mapper = mapper;
    }

    /** Stores an image anyone with the URL can load; returns that URL. */
    public String uploadPublicImage(byte[] bytes, String publicId) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId);
        return upload("image", bytes, params).path("secure_url").asString(null);
    }

    /** Stores a file only this server can read back. */
    public void uploadPrivateFile(byte[] bytes, String publicId) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId);
        params.put("type", AUTHENTICATED);
        upload("raw", bytes, params);
    }

    /** The file's bytes, or null when it does not exist. Read through the signed download API, which only this server can call. */
    public byte[] downloadPrivateFile(String publicId) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId);
        params.put("type", AUTHENTICATED);
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        try {
            return client.get().uri(API + props.cloudName() + "/raw/download?public_id={id}&type={type}&timestamp={ts}&api_key={key}&signature={sig}",
                    publicId, AUTHENTICATED, params.get("timestamp"), props.apiKey(), signature(params, props.apiSecret())).retrieve().body(byte[].class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw new StorageUnavailableException("Cloudinary answered " + e.getStatusCode().value() + " to a download.", e);
        } catch (RestClientException e) {
            throw new StorageUnavailableException("Cloudinary could not be reached.", e);
        }
    }

    public void deletePrivateFile(String publicId) {
        destroy("raw", publicId, AUTHENTICATED);
    }

    public void deletePublicImage(String publicId) {
        destroy("image", publicId, "upload");
    }

    private JsonNode upload(String resourceType, byte[] bytes, Map<String, String> params) {
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        MultiValueMap<String, Object> form = signedForm(params);
        form.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "upload";
            }
        });
        return post(API + props.cloudName() + "/" + resourceType + "/upload", form);
    }

    private void destroy(String resourceType, String publicId, String type) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId);
        params.put("type", type);
        params.put("invalidate", "true");
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        post(API + props.cloudName() + "/" + resourceType + "/destroy", signedForm(params));
    }

    private MultiValueMap<String, Object> signedForm(Map<String, String> params) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);
        form.add("api_key", props.apiKey());
        form.add("signature", signature(params, props.apiSecret()));
        return form;
    }

    private JsonNode post(String url, MultiValueMap<String, Object> form) {
        try {
            String body = client.post().uri(url).contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(String.class);
            return mapper.readTree(body);
        } catch (RestClientResponseException e) {
            throw new StorageUnavailableException("Cloudinary answered " + e.getStatusCode().value() + ".", e);
        } catch (RestClientException e) {
            throw new StorageUnavailableException("Cloudinary could not be reached.", e);
        }
    }

    /** The upload API's signature: SHA-1 of the sorted "k=v" pairs joined by "&", followed by the secret. */
    static String signature(Map<String, String> params, String secret) {
        StringBuilder toSign = new StringBuilder();
        new TreeMap<>(params).forEach((key, value) -> toSign.append(toSign.isEmpty() ? "" : "&").append(key).append('=').append(value));
        return HexFormat.of().formatHex(sha1(toSign + secret));
    }

    private static byte[] sha1(String value) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
