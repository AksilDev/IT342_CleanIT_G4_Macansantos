import { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../../api/axios';

interface OAuthUserData {
  email: string;
  name: string;
  tempToken?: string;
  supabaseToken?: string;
}

export default function RoleSelection() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [userData, setUserData] = useState<OAuthUserData | null>(null);
  const [selectedRole, setSelectedRole] = useState<'client' | 'technician' | null>(null);
  const [contact, setContact] = useState('');
  
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [uploadingImage, setUploadingImage] = useState(false);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const email = searchParams.get('email');
    const name = searchParams.get('name');
    const tempToken = searchParams.get('tempToken');

    if (!email || !name) {
      navigate('/login?error=missing_oauth_data');
      return;
    }

    const supabaseToken = searchParams.get('supabaseToken');
    setUserData({ email, name, tempToken: tempToken || undefined, supabaseToken: supabaseToken || undefined });
  }, [searchParams, navigate]);

  const canSubmit = useMemo(() => {
    return selectedRole !== null && contact.trim().length > 0 && imageFile !== null;
  }, [selectedRole, contact, imageFile]);

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        setError('Image size must be less than 5MB');
        return;
      }
      if (!file.type.startsWith('image/')) {
        setError('Please select an image file');
        return;
      }
      setImageFile(file);
      setError('');
      
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const uploadImage = async (): Promise<string | null> => {
    if (!imageFile) return null;
    
    setUploadingImage(true);
    try {
      const formData = new FormData();
      formData.append('file', imageFile);
      
      const response = await api.post('/v1/auth/upload-image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      return response.data?.imageUrl;
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to upload image');
      return null;
    } finally {
      setUploadingImage(false);
    }
  };

  const handleRoleSubmit = async () => {
    if (!canSubmit || !userData) return;

    setLoading(true);
    setError('');

    try {
      // Upload image first
      const uploadedImageUrl = await uploadImage();
      if (!uploadedImageUrl) {
        setLoading(false);
        return;
      }

      const response = await api.post('/v1/auth/oauth-complete', {
        email: userData.email,
        name: userData.name,
        role: selectedRole,
        tempToken: userData.tempToken,
        contactNo: contact,
        imageUrl: uploadedImageUrl
      }, {
        headers: {
          Authorization: `Bearer ${userData.supabaseToken}`
        }
      });

      // Save the JWT so protected routes work
      const { token, name, email, role, verified } = response.data;
      localStorage.setItem('token', token);
      localStorage.setItem('cleanit.user', JSON.stringify({ name, email, role, verified }));

      if (selectedRole === 'technician') {
        navigate('/dashboard/technician');
      } else {
        navigate('/dashboard');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to complete registration');
    } finally {
      setLoading(false);
    }
  };

  if (!userData) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-white">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-[radial-gradient(circle_at_20%_10%,rgba(124,58,237,0.35),transparent_45%),radial-gradient(circle_at_80%_80%,rgba(99,102,241,0.25),transparent_45%),linear-gradient(135deg,rgba(15,23,42,0.95),rgba(3,7,18,0.95))] overflow-y-auto py-10">
      <div className="mx-auto flex min-h-screen max-w-6xl flex-col items-center justify-center px-4">
        <div className="mb-8 text-center mt-10">
          <div className="text-sm font-semibold tracking-widest text-white/70">ACCOUNT SETUP</div>
          <h1 className="mt-2 text-2xl font-semibold text-white">Welcome, {userData.name}!</h1>
          <p className="mt-2 text-slate-300">Complete your details to finish setting up your account</p>
        </div>

        <div className="w-full max-w-md space-y-4 mb-20">
          <div className="rounded-xl bg-slate-800/50 backdrop-blur-sm p-6 border border-slate-700">
            <div className="mb-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-12 w-12 rounded-full bg-violet-600 flex items-center justify-center shrink-0">
                  <span className="text-white font-semibold text-lg">
                    {userData.name.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div className="overflow-hidden">
                  <div className="text-white font-medium truncate">{userData.name}</div>
                  <div className="text-slate-400 text-sm truncate">{userData.email}</div>
                </div>
              </div>
            </div>

            <div className="space-y-5">
              {/* Role Selection */}
              <div className="space-y-3">
                <label className="text-sm font-medium text-slate-300">Choose your role <span className="text-rose-500">*</span></label>
                <div className="grid grid-cols-2 gap-3">
                  <label className="block">
                    <input
                      type="radio"
                      name="role"
                      value="client"
                      checked={selectedRole === 'client'}
                      onChange={(e) => setSelectedRole(e.target.value as 'client')}
                      className="sr-only peer"
                    />
                    <div className="peer-checked:ring-2 peer-checked:ring-violet-500 peer-checked:bg-violet-600/20 rounded-lg p-3 border border-slate-600 cursor-pointer transition-all hover:bg-slate-700/50 text-center h-full">
                      <div className="text-2xl mb-1">👤</div>
                      <div className="text-white font-medium text-sm">Client</div>
                    </div>
                  </label>

                  <label className="block">
                    <input
                      type="radio"
                      name="role"
                      value="technician"
                      checked={selectedRole === 'technician'}
                      onChange={(e) => setSelectedRole(e.target.value as 'technician')}
                      className="sr-only peer"
                    />
                    <div className="peer-checked:ring-2 peer-checked:ring-violet-500 peer-checked:bg-violet-600/20 rounded-lg p-3 border border-slate-600 cursor-pointer transition-all hover:bg-slate-700/50 text-center h-full">
                      <div className="text-2xl mb-1">🔧</div>
                      <div className="text-white font-medium text-sm">Technician</div>
                    </div>
                  </label>
                </div>
              </div>

              {/* Contact Number */}
              <div>
                <label className="text-sm font-medium text-slate-300">Contact Number <span className="text-rose-500">*</span></label>
                <input
                  type="text"
                  value={contact}
                  onChange={(e) => setContact(e.target.value)}
                  placeholder="+63 0000 000 000"
                  className="mt-1 w-full rounded-lg border border-slate-600 bg-slate-900/50 px-4 py-3 text-sm text-white outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition-colors"
                />
              </div>

              {/* ID Verification Image */}
              <div>
                <label className="text-sm font-medium text-slate-300">ID Verification Image <span className="text-rose-500">*</span></label>
                <p className="text-xs text-slate-400 mt-1">Upload a valid ID (Driver's License, Passport, National ID)</p>
                <div className="mt-3">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleImageSelect}
                    className="hidden"
                  />
                  <div className="flex items-center gap-4">
                    {imagePreview ? (
                      <div className="relative shrink-0">
                        <img
                          src={imagePreview}
                          alt="ID Preview"
                          className="h-20 w-28 rounded-lg object-cover border border-slate-600"
                        />
                        <button
                          type="button"
                          onClick={() => {
                            setImageFile(null);
                            setImagePreview(null);
                          }}
                          className="absolute -top-2 -right-2 h-6 w-6 rounded-full bg-rose-500 text-white text-xs hover:bg-rose-600 flex items-center justify-center shadow-lg"
                        >
                          ✕
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        className="flex h-20 w-28 shrink-0 items-center justify-center rounded-lg border border-dashed border-slate-500 bg-slate-800/50 text-slate-400 hover:border-violet-400 hover:text-violet-400 transition-colors"
                      >
                        <span className="text-2xl">🪪</span>
                      </button>
                    )}
                    <div className="flex-1">
                      <button
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        className="rounded-lg border border-slate-600 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-300 hover:bg-slate-700 hover:text-white transition-colors w-full"
                      >
                        {imagePreview ? 'Change Image' : 'Select Image'}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {error && (
              <div className="mt-5 p-3 rounded-lg bg-red-500/10 border border-red-500/50">
                <div className="text-red-400 text-sm">{error}</div>
              </div>
            )}

            <button
              onClick={handleRoleSubmit}
              disabled={!canSubmit || loading || uploadingImage}
              className="w-full mt-6 rounded-xl bg-violet-600 px-4 py-3 text-sm font-semibold text-white shadow-lg transition-all hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {uploadingImage ? 'Uploading Image...' : loading ? 'Creating Account...' : 'Complete Account Setup'}
            </button>

            <div className="mt-4 text-center">
              <button
                onClick={() => navigate('/login')}
                className="text-slate-400 text-sm hover:text-white transition-colors"
              >
                Cancel and return to login
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
