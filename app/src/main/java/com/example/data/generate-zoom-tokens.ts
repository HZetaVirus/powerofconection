import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { create, getNumericDate } from "https://deno.land/x/djwt@v2.8/mod.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// Zoom Meeting SDK JWT generation requirements:
// Header: { "alg": "HS256", "typ": "JWT" }
// Payload: {
//   "sdkKey": "CLIENT_ID",
//   "mn": meetingNumber,
//   "role": role (0 for participant, 1 for host),
//   "iat": epoch time in seconds,
//   "exp": iat + duration,
//   "tokenExp": exp
// }

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  // Handle CORS
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // 1. Get Supabase client to verify authorization
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const supabase = createClient(supabaseUrl, supabaseAnonKey);

    // Get Auth header
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Authorization header missing" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Verify user profile
    const token = authHeader.replace("Bearer ", "");
    const { data: { user }, error: authError } = await supabase.auth.getUser(token);
    
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Invalid auth token" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 2. Fetch User Role to check permissions
    const { data: roleData, error: roleError } = await supabase
      .from("roles")
      .select("role")
      .eq("user_id", user.email)
      .single();

    if (roleError || !roleData) {
      return new Response(JSON.stringify({ error: "User role not found" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const isAdmin = roleData.role === "admin" || roleData.role === "super_admin";

    // 3. Get Zoom Credentials from Environment
    const zoomClientId = Deno.env.get("ZOOM_CLIENT_ID");
    const zoomClientSecret = Deno.env.get("ZOOM_CLIENT_SECRET");

    if (!zoomClientId || !zoomClientSecret) {
      return new Response(
        JSON.stringify({ error: "Zoom credentials are not configured in environment" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Parse request parameters
    const body = await req.json().catch(() => ({}));
    const meetingNumber = body.meetingNumber ? parseInt(body.meetingNumber) : 123456789;
    // Role: 1 for admin/host, 0 for student/participant
    const zoomRole = isAdmin ? 1 : 0;

    // 5. Generate Zoom SDK JWT
    const key = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(zoomClientSecret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    const iat = Math.floor(Date.now() / 1000) - 30; // 30s buffer
    const exp = iat + 60 * 60 * 2; // Valid for 2 hours

    const payload = {
      sdkKey: zoomClientId,
      mn: meetingNumber,
      role: zoomRole,
      iat: iat,
      exp: exp,
      tokenExp: exp
    };

    const sdkJwtToken = await create(
      { alg: "HS256", typ: "JWT" },
      payload,
      key
    );

    // 6. Generate ZAK Token if Host/Admin
    let zakToken = "";
    if (isAdmin) {
      // Authenticate with Zoom via OAuth Server-to-Server to get ZAK token
      const tokenResponse = await fetch("https://zoom.us/oauth/token?grant_type=account_credentials&account_id=" + Deno.env.get("ZOOM_ACCOUNT_ID"), {
        method: "POST",
        headers: {
          "Authorization": "Basic " + btoa(`${zoomClientId}:${zoomClientSecret}`),
        }
      });

      if (tokenResponse.ok) {
        const tokenData = await tokenResponse.json();
        const accessToken = tokenData.access_token;

        // Get Host user token (ZAK)
        const zakResponse = await fetch("https://api.zoom.us/v2/users/me/token?type=zak", {
          headers: {
            "Authorization": `Bearer ${accessToken}`,
          }
        });

        if (zakResponse.ok) {
          const zakData = await zakResponse.json();
          zakToken = zakData.token;
        }
      }
    }

    return new Response(
      JSON.stringify({
        sdkKey: zoomClientId,
        signature: sdkJwtToken,
        zakToken: zakToken,
        isAdmin: isAdmin
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );

  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
